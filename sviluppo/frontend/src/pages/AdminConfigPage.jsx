import { useEffect, useState } from 'react';
import { api, errorsByField } from '../api.js';
import AdminLayout from '../components/Admin/AdminLayout';
import AdminField from '../components/Admin/AdminField';

// Configurazione del gioco: fazioni e vantaggi, limiti delle rarità, punti vita e round della modalità,
// soglie e premi delle leghe. Codici, ordine e carta Lord non cambiano; i cambi valgono da subito
// (anche nelle pagine pubbliche: GET /factions, GET /rules). Le missioni arrivano con la #125.
export default function AdminConfigPage() {
  const [config, setConfig] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([api('/admin/factions'), api('/admin/rarities'), api('/admin/game-modes'), api('/admin/leagues'), api('/rules')])
      .then(([factions, rarities, gameModes, leagues, rules]) => setConfig({ factions, rarities, gameModes, leagues, deckSize: rules.deckSize }))
      .catch(setError);
  }, []);

  return (
    <AdminLayout title="Configurazione">
      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}
      {!error && !config && <p className="collection-message">Caricamento della configurazione...</p>}
      {config && (
        <>
          <nav className="admin-row-actions" aria-label="Parti della configurazione" style={{ marginBottom: 24 }}>
            <a className="admin-button admin-button--small" href="#fazioni">Fazioni</a>
            <a className="admin-button admin-button--small" href="#rarita">Rarità</a>
            <a className="admin-button admin-button--small" href="#modalita">Modalità</a>
            <a className="admin-button admin-button--small" href="#leghe">Leghe</a>
          </nav>

          <section id="fazioni" className="admin-panel" aria-labelledby="factions-title">
            <h2 id="factions-title">Fazioni</h2>
            <p className="admin-panel__intro">Nome, testi e vantaggi tra fazioni: il bonus vale per la creatura della fazione contro quella indicata. La carta Lord non si cambia da qui.</p>
            {config.factions.map((faction) => (
              <FactionForm key={faction.code} faction={faction} factions={config.factions} />
            ))}
          </section>

          <RaritiesForm rarities={config.rarities} deckSize={config.deckSize} />

          <section id="modalita" className="admin-panel" aria-labelledby="modes-title">
            <h2 id="modes-title">Modalità di gioco</h2>
            <p className="admin-panel__intro">I valori valgono per le partite giocate da adesso: quelle già giocate tengono i loro. Le carte per mazzo ({config.deckSize}) non si cambiano.</p>
            {config.gameModes.map((mode) => <GameModeForm key={mode.code} mode={mode} />)}
          </section>

          <LeaguesForm leagues={config.leagues} />

          <section className="admin-panel" aria-labelledby="missions-title">
            <h2 id="missions-title">Missioni</h2>
            <p className="admin-panel__intro">I premi delle missioni si cambieranno da qui quando ci saranno le missioni (#125).</p>
          </section>
        </>
      )}
    </AdminLayout>
  );
}

// Esito di un salvataggio sotto il suo form
function SaveResult({ error, saved }) {
  if (error) return <p className="admin-message admin-message--error" role="alert">{error.message}</p>;
  if (saved) return <p className="admin-message admin-message--ok" role="status">Salvato.</p>;
  return null;
}

// Una fazione: PUT /admin/factions/{code}. I vantaggi mandati sostituiscono quelli di prima
function FactionForm({ faction, factions }) {
  const [values, setValues] = useState({
    name: faction.name,
    description: faction.description ?? '',
    lordTitle: faction.lordTitle ?? '',
    advantages: faction.advantages.map((advantage) => ({ ...advantage })),
  });
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = errorsByField(error);
  const others = factions.filter((item) => item.code !== faction.code);

  const set = (name) => (event) => setValues((current) => ({ ...current, [name]: event.target.value }));
  const setAdvantage = (index, change) => setValues((current) => ({
    ...current,
    advantages: current.advantages.map((advantage, position) => (position === index ? { ...advantage, ...change } : advantage)),
  }));

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const updated = await api(`/admin/factions/${faction.code}`, {
        method: 'PUT',
        body: {
          name: values.name.trim(),
          description: values.description.trim() || null,
          lordTitle: values.lordTitle.trim() || null,
          advantages: values.advantages.map((advantage) => ({
            targetFaction: advantage.targetFaction,
            attackBonus: Number(advantage.attackBonus),
            defenseBonus: Number(advantage.defenseBonus),
          })),
        },
      });
      setValues((current) => ({ ...current, advantages: updated.advantages.map((advantage) => ({ ...advantage })) }));
      setSaved(true);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="admin-slot" onSubmit={save} noValidate>
      <h3 style={{ marginTop: 0 }}>{faction.name} <small className="collection-message">({faction.code})</small></h3>
      <div className="admin-form">
        <AdminField label="Nome" error={fieldErrors.name}>
          <input value={values.name} onChange={set('name')} maxLength={50} required aria-invalid={Boolean(fieldErrors.name)} />
        </AdminField>
        <AdminField label="Titolo del Lord" error={fieldErrors.lordTitle} hint={faction.lordCard ? `Carta Lord: ${faction.lordCard.code} · ${faction.lordCard.name}` : 'Nessuna carta Lord.'}>
          <input value={values.lordTitle} onChange={set('lordTitle')} maxLength={50} aria-invalid={Boolean(fieldErrors.lordTitle)} />
        </AdminField>
        <AdminField label="Descrizione (pagina Il gioco)" error={fieldErrors.description} wide>
          <textarea value={values.description} onChange={set('description')} maxLength={2000} aria-invalid={Boolean(fieldErrors.description)} />
        </AdminField>
      </div>

      <h3>Forte contro</h3>
      {values.advantages.length === 0 && <p className="collection-message">Nessun vantaggio.</p>}
      {values.advantages.map((advantage, index) => (
        <div key={index} className="admin-slot__rarity" style={{ gridTemplateColumns: '1fr 120px 120px auto' }}>
          <select value={advantage.targetFaction} onChange={(event) => setAdvantage(index, { targetFaction: event.target.value })} aria-label="Fazione contro cui vale il vantaggio" aria-invalid={Boolean(fieldErrors[`advantages[${index}].targetFaction`])}>
            {others.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}
          </select>
          <input type="number" min="0" max="20" value={advantage.attackBonus} onChange={(event) => setAdvantage(index, { attackBonus: event.target.value })} aria-label="Bonus di attacco" title="Bonus di attacco" />
          <input type="number" min="0" max="20" value={advantage.defenseBonus} onChange={(event) => setAdvantage(index, { defenseBonus: event.target.value })} aria-label="Bonus di difesa" title="Bonus di difesa" />
          <button type="button" className="admin-button admin-button--small" onClick={() => setValues((current) => ({ ...current, advantages: current.advantages.filter((_, position) => position !== index) }))}>Togli</button>
        </div>
      ))}
      {values.advantages.length > 0 && <small className="collection-message">Colonne: fazione, bonus di attacco, bonus di difesa.</small>}

      <div className="admin-form-actions">
        {values.advantages.length < others.length && (
          <button
            type="button"
            className="admin-button admin-button--small"
            onClick={() => setValues((current) => ({
              ...current,
              advantages: [...current.advantages, { targetFaction: others.find((item) => !current.advantages.some((advantage) => advantage.targetFaction === item.code))?.code ?? others[0].code, attackBonus: 1, defenseBonus: 0 }],
            }))}
          >
            Aggiungi un vantaggio
          </button>
        )}
        <button type="submit" className="admin-button admin-button--filled" disabled={saving}>{saving ? 'Salvataggio...' : `Salva ${faction.name}`}</button>
      </div>
      <SaveResult error={error} saved={saved} />
    </form>
  );
}

// Limiti delle rarità nei mazzi: PUT /admin/rarities/{code}, una riga alla volta
function RaritiesForm({ rarities, deckSize }) {
  const [rows, setRows] = useState(() => rarities.map((rarity) => ({ ...rarity, maxPerDeck: rarity.maxPerDeck ?? '' })));
  // I valori salvati, per capire se un limite si sta abbassando
  const [savedValues, setSavedValues] = useState(() => Object.fromEntries(rarities.map((rarity) => [rarity.code, rarity])));
  const [savingCode, setSavingCode] = useState(null);
  const [result, setResult] = useState({});

  const setRow = (code, change) => setRows((current) => current.map((row) => (row.code === code ? { ...row, ...change } : row)));

  const save = async (row) => {
    const before = savedValues[row.code];
    const lowered = Number(row.maxCopies) < before.maxCopies
      || (row.maxPerDeck !== '' && (before.maxPerDeck === null || Number(row.maxPerDeck) < before.maxPerDeck));
    // Un limite più basso può rendere non validi dei mazzi salvati: si chiede prima
    if (lowered && !window.confirm(`Abbassare i limiti delle ${row.name}? I mazzi che non li rispettano più diventeranno non validi finché i giocatori non li sistemano.`)) {
      return;
    }
    setSavingCode(row.code);
    setResult((current) => ({ ...current, [row.code]: null }));
    try {
      const updated = await api(`/admin/rarities/${row.code}`, {
        method: 'PUT',
        body: { name: row.name.trim(), maxPerDeck: row.maxPerDeck === '' ? null : Number(row.maxPerDeck), maxCopies: Number(row.maxCopies) },
      });
      setSavedValues((current) => ({ ...current, [row.code]: updated }));
      setResult((current) => ({ ...current, [row.code]: { saved: true } }));
    } catch (err) {
      setResult((current) => ({ ...current, [row.code]: { error: err } }));
    } finally {
      setSavingCode(null);
    }
  };

  const failed = rows.map((row) => result[row.code]?.error).find(Boolean);

  return (
    <section id="rarita" className="admin-panel" aria-labelledby="rarities-title">
      <h2 id="rarities-title">Rarità</h2>
      <p className="admin-panel__intro">Quante carte di ogni rarità al massimo in un mazzo di {deckSize} carte (vuoto = nessun limite) e quante copie della stessa carta. I limiti valgono da subito anche per i mazzi già salvati.</p>
      {failed && <p className="admin-message admin-message--error" role="alert">{failed.message}</p>}
      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr>
              <th scope="col">Rarità</th>
              <th scope="col">Nome</th>
              <th scope="col">Al massimo per mazzo</th>
              <th scope="col">Copie della stessa carta</th>
              <th scope="col"><span className="visually-hidden">Azioni</span></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.code}>
                <td>{row.code}</td>
                <td><input value={row.name} onChange={(event) => setRow(row.code, { name: event.target.value })} maxLength={50} aria-label={`Nome di ${row.code}`} /></td>
                <td><input type="number" min="0" max="100" value={row.maxPerDeck} placeholder="Nessun limite" onChange={(event) => setRow(row.code, { maxPerDeck: event.target.value })} aria-label={`Carte ${row.name} al massimo per mazzo`} /></td>
                <td><input type="number" min="1" max="20" value={row.maxCopies} onChange={(event) => setRow(row.code, { maxCopies: event.target.value })} aria-label={`Copie della stessa carta ${row.name}`} /></td>
                <td>
                  <div className="admin-row-actions">
                    <button type="button" className="admin-button admin-button--small" onClick={() => save(row)} disabled={savingCode === row.code}>
                      {savingCode === row.code ? '...' : 'Salva'}
                    </button>
                    {result[row.code]?.saved && <span className="admin-status admin-status--on">Salvata</span>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

// Una modalità: PUT /admin/game-modes/{code}
function GameModeForm({ mode }) {
  const [values, setValues] = useState({
    name: mode.name,
    description: mode.description ?? '',
    startingLifePoints: mode.startingLifePoints,
    roundLimit: mode.roundLimit,
  });
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = errorsByField(error);

  const set = (name) => (event) => setValues((current) => ({ ...current, [name]: event.target.value }));

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      await api(`/admin/game-modes/${mode.code}`, {
        method: 'PUT',
        body: {
          name: values.name.trim(),
          description: values.description.trim() || null,
          startingLifePoints: Number(values.startingLifePoints),
          roundLimit: Number(values.roundLimit),
        },
      });
      setSaved(true);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="admin-slot" onSubmit={save} noValidate>
      <h3 style={{ marginTop: 0 }}>{mode.name} <small className="collection-message">({mode.code}{mode.isActive ? ', attiva' : ', spenta'})</small></h3>
      <div className="admin-form">
        <AdminField label="Nome" error={fieldErrors.name}>
          <input value={values.name} onChange={set('name')} maxLength={60} required aria-invalid={Boolean(fieldErrors.name)} />
        </AdminField>
        <AdminField label="Descrizione" error={fieldErrors.description}>
          <input value={values.description} onChange={set('description')} maxLength={255} aria-invalid={Boolean(fieldErrors.description)} />
        </AdminField>
        <AdminField label="Punti vita iniziali" error={fieldErrors.startingLifePoints} hint="Di ogni giocatore, da 1 a 1000.">
          <input type="number" min="1" max="1000" value={values.startingLifePoints} onChange={set('startingLifePoints')} required aria-invalid={Boolean(fieldErrors.startingLifePoints)} />
        </AdminField>
        <AdminField label="Limite di round" error={fieldErrors.roundLimit} hint="Dopo questi round la partita finisce in pareggio.">
          <input type="number" min="1" max="1000" value={values.roundLimit} onChange={set('roundLimit')} required aria-invalid={Boolean(fieldErrors.roundLimit)} />
        </AdminField>
      </div>
      <div className="admin-form-actions">
        <button type="submit" className="admin-button admin-button--filled" disabled={saving}>{saving ? 'Salvataggio...' : `Salva ${mode.name}`}</button>
      </div>
      <SaveResult error={error} saved={saved} />
    </form>
  );
}

// Soglie e premi delle leghe: PUT /admin/leagues con tutte le leghe insieme
function LeaguesForm({ leagues }) {
  const [rows, setRows] = useState(() => leagues.map((league) => ({ ...league })));
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = errorsByField(error);

  const setRow = (index, change) => setRows((current) => current.map((row, position) => (position === index ? { ...row, ...change } : row)));

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const updated = await api('/admin/leagues', {
        method: 'PUT',
        body: { leagues: rows.map((row) => ({ code: row.code, minRating: Number(row.minRating), rewardCredits: Number(row.rewardCredits) })) },
      });
      setRows(updated.map((league) => ({ ...league })));
      setSaved(true);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section id="leghe" className="admin-panel" aria-labelledby="leagues-title">
      <h2 id="leagues-title">Leghe</h2>
      <p className="admin-panel__intro">Il punteggio da cui si entra in ogni lega e il premio in Dream Credits della prima volta. La prima lega parte sempre da 0 ed è quella di partenza, senza premio; ogni lega parte da un punteggio più alto della precedente.</p>
      <form onSubmit={save} noValidate>
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th scope="col">Lega</th>
                <th scope="col">Punteggio minimo</th>
                <th scope="col">Premio (DC)</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => {
                const isFirst = index === 0;
                return (
                  <tr key={row.code}>
                    <td>{row.name}</td>
                    <td>
                      <input type="number" min="0" value={row.minRating} onChange={(event) => setRow(index, { minRating: event.target.value })} readOnly={isFirst} aria-label={`Punteggio minimo di ${row.name}`} aria-invalid={Boolean(fieldErrors[`leagues[${index}].minRating`])} />
                    </td>
                    <td>
                      <input type="number" min="0" value={row.rewardCredits} onChange={(event) => setRow(index, { rewardCredits: event.target.value })} readOnly={isFirst} aria-label={`Premio di ${row.name}`} aria-invalid={Boolean(fieldErrors[`leagues[${index}].rewardCredits`])} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="admin-form-actions">
          <button type="submit" className="admin-button admin-button--filled" disabled={saving}>{saving ? 'Salvataggio...' : 'Salva le leghe'}</button>
        </div>
        <SaveResult error={error} saved={saved} />
      </form>
    </section>
  );
}
