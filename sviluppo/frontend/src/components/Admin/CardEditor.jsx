import { useState } from 'react';
import { api, errorsByField } from '../../api.js';
import AdminField from './AdminField';
import ImageUpload from './ImageUpload';
import { factionNames } from './adminFormat';

const DEFAULT_FOCUS_Y = 0.4;

// I valori del form partendo da una carta del backoffice (GET /admin/cards); per una carta nuova vuoti
function initialValues(card, fullArt) {
  return {
    code: card?.code ?? '',
    baseCardId: card?.baseCardId ?? '',
    name: card?.name ?? '',
    description: card?.description ?? '',
    attack: card?.attack ?? '',
    defense: card?.defense ?? '',
    factionCode: card?.factionCode ?? '',
    rarityCode: card?.rarityCode ?? (fullArt ? 'LEGENDARY' : ''),
    cardSetCode: card?.cardSetCode ?? 'BASE',
    effectCode: card?.effectCode ?? '',
    effectValue: card?.effectValue ?? '',
    illustrationUrl: card?.illustrationUrl ?? '',
    illustrationThumbUrl: card?.illustrationThumbUrl ?? '',
    illustrationFocusY: card?.illustrationFocusY ?? DEFAULT_FOCUS_Y,
    isActive: card?.isActive ?? true,
  };
}

const orNull = (value) => (value === '' || value === undefined ? null : value);
const numberOrNull = (value) => (value === '' || value === null ? null : Number(value));

// Il corpo di POST e PUT /admin/cards. Con PUT si mandano tutti i dati: quelli che mancano si svuotano.
// La full art manda solo codice, carta base, illustrazione e stato: il resto lo prende dalla carta base.
function requestBody(values, fullArt, effect) {
  const artwork = {
    illustrationUrl: values.illustrationUrl,
    illustrationThumbUrl: values.illustrationThumbUrl,
    illustrationFocusY: Number(values.illustrationFocusY),
    isActive: values.isActive,
  };
  if (fullArt) {
    return { code: values.code.trim(), baseCardId: orNull(values.baseCardId), ...artwork };
  }
  return {
    code: values.code.trim(),
    name: values.name.trim(),
    description: orNull(values.description.trim()),
    attack: numberOrNull(values.attack),
    defense: numberOrNull(values.defense),
    factionCode: orNull(values.factionCode),
    rarityCode: orNull(values.rarityCode),
    cardSetCode: orNull(values.cardSetCode),
    effectCode: orNull(values.effectCode),
    effectValue: effect?.hasValue ? numberOrNull(values.effectValue) : null,
    ...artwork,
  };
}

// Scheda di una carta del backoffice: nuova carta, nuova full art o modifica (POST/PUT /admin/cards).
// card: la carta da modificare, oppure null per una nuova; fullArt: per una full art nuova.
// options: { factions, rarities, effects, cardSets, legendaries } per le scelte del form.
export default function CardEditor({ card, fullArt: newFullArt = false, options, onSaved, onClose }) {
  const isNew = !card;
  const fullArt = isNew ? newFullArt : card.variantType === 'FULL_ART';
  const [values, setValues] = useState(() => initialValues(card, fullArt));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = errorsByField(error);
  const effect = options.effects.find((item) => item.code === values.effectCode);

  const set = (name) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setValues((current) => ({ ...current, [name]: value }));
  };

  // Full art nuova: il codice è quello della carta base con -FA
  const chooseBase = (event) => {
    const base = options.legendaries.find((item) => item.id === event.target.value);
    setValues((current) => ({ ...current, baseCardId: event.target.value, code: base ? `${base.code}-FA` : '' }));
  };

  const save = async (event) => {
    event.preventDefault();
    // Disattivare una carta usata rende non validi i mazzi che la contengono: si chiede prima
    if (card?.isActive && !values.isActive && card.deckCount > 0
      && !window.confirm(`La carta è in ${card.deckCount} mazzi: diventeranno non validi finché i giocatori non li sistemano. Disattivarla?`)) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body = requestBody(values, fullArt, effect);
      const saved = isNew
        ? await api('/admin/cards', { method: 'POST', body })
        : await api(`/admin/cards/${card.id}`, { method: 'PUT', body });
      onSaved(saved, isNew ? 'Carta creata.' : 'Carta salvata.');
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  const title = isNew ? (fullArt ? 'Nuova full art' : 'Nuova carta') : `${card.code}${card.name ? ` · ${card.name}` : ''}`;
  const focusPercent = Math.round(Number(values.illustrationFocusY) * 100);

  return (
    <section className="admin-panel admin-editor" aria-labelledby="card-editor-title">
      <div className="admin-editor__head">
        <h2 id="card-editor-title">{title}</h2>
        <button type="button" className="admin-button admin-button--small" onClick={onClose}>Chiudi</button>
      </div>

      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}

      <form className="admin-form" onSubmit={save} noValidate>
        {fullArt && isNew && (
          <AdminField label="Carta base (Leggendaria senza full art)" error={fieldErrors.baseCardId} wide>
            <select value={values.baseCardId} onChange={chooseBase} required aria-invalid={Boolean(fieldErrors.baseCardId)}>
              <option value="">Scegli la carta</option>
              {options.legendaries.map((base) => <option key={base.id} value={base.id}>{base.code} · {base.name}</option>)}
            </select>
          </AdminField>
        )}

        <AdminField label="Codice" error={fieldErrors.code} hint={isNew ? 'Lettere maiuscole, numeri e trattini, es. BASE-100. Poi non si cambia.' : 'Il codice non si cambia.'}>
          <input value={values.code} onChange={set('code')} readOnly={!isNew || fullArt} maxLength={30} required aria-invalid={Boolean(fieldErrors.code)} />
        </AdminField>

        {!fullArt && (
          <>
            <AdminField label="Nome" error={fieldErrors.name}>
              <input value={values.name} onChange={set('name')} maxLength={80} required aria-invalid={Boolean(fieldErrors.name)} />
            </AdminField>

            <AdminField label="Descrizione (testo di colore)" error={fieldErrors.description} wide>
              <textarea value={values.description} onChange={set('description')} maxLength={500} aria-invalid={Boolean(fieldErrors.description)} />
            </AdminField>

            <AdminField label="Attacco (ATT)" error={fieldErrors.attack} hint="Da 1 a 99. Vale anche per la full art.">
              <input type="number" min="1" max="99" value={values.attack} onChange={set('attack')} required aria-invalid={Boolean(fieldErrors.attack)} />
            </AdminField>

            <AdminField label="Difesa (DIF)" error={fieldErrors.defense} hint="Da 1 a 99.">
              <input type="number" min="1" max="99" value={values.defense} onChange={set('defense')} required aria-invalid={Boolean(fieldErrors.defense)} />
            </AdminField>

            <AdminField label="Fazione" error={fieldErrors.factionCode}>
              <select value={values.factionCode} onChange={set('factionCode')} required aria-invalid={Boolean(fieldErrors.factionCode)}>
                <option value="">Scegli</option>
                {options.factions.map((faction) => <option key={faction.code} value={faction.code}>{faction.name ?? factionNames[faction.code]}</option>)}
              </select>
            </AdminField>

            <AdminField label="Rarità" error={fieldErrors.rarityCode}>
              <select value={values.rarityCode} onChange={set('rarityCode')} required aria-invalid={Boolean(fieldErrors.rarityCode)}>
                <option value="">Scegli</option>
                {options.rarities.map((rarity) => <option key={rarity.code} value={rarity.code}>{rarity.name}</option>)}
              </select>
            </AdminField>

            <AdminField label="Espansione" error={fieldErrors.cardSetCode}>
              <select value={values.cardSetCode} onChange={set('cardSetCode')} required aria-invalid={Boolean(fieldErrors.cardSetCode)}>
                {options.cardSets.map((cardSet) => <option key={cardSet.code} value={cardSet.code}>{cardSet.name}</option>)}
              </select>
            </AdminField>

            <AdminField label="Effetto" error={fieldErrors.effectCode} hint={effect?.description}>
              <select value={values.effectCode} onChange={set('effectCode')} aria-invalid={Boolean(fieldErrors.effectCode)}>
                <option value="">Nessun effetto</option>
                {options.effects.map((item) => <option key={item.code} value={item.code}>{item.name}{item.hasValue ? ' X' : ''}</option>)}
              </select>
            </AdminField>

            {effect?.hasValue && (
              <AdminField label={`Valore X di ${effect.name}`} error={fieldErrors.effectValue} hint="Da 1 a 20.">
                <input type="number" min="1" max="20" value={values.effectValue} onChange={set('effectValue')} required aria-invalid={Boolean(fieldErrors.effectValue)} />
              </AdminField>
            )}
          </>
        )}

        <div className="admin-artwork is-wide">
          <div className={`admin-artwork__preview${fullArt ? ' admin-artwork__preview--tall' : ''}`}>
            {values.illustrationThumbUrl || values.illustrationUrl
              ? <img src={values.illustrationThumbUrl || values.illustrationUrl} alt="Anteprima dell'illustrazione" style={{ objectPosition: `50% ${focusPercent}%` }} />
              : <span>Nessuna illustrazione</span>}
          </div>
          <div className="admin-artwork__controls">
            <ImageUpload
              code={values.code.trim()}
              type={fullArt ? 'FULL_ART' : 'ILLUSTRATION'}
              label={values.illustrationUrl ? "Cambia l'illustrazione" : "Carica l'illustrazione"}
              onUploaded={({ url, thumbUrl }) => setValues((current) => ({ ...current, illustrationUrl: url, illustrationThumbUrl: thumbUrl ?? url }))}
            />
            <AdminField label={`Ritaglio: ${focusPercent}% dall'alto`} error={fieldErrors.illustrationFocusY} hint="Quale parte dell'immagine si vede nella finestra della carta.">
              <input type="range" min="0" max="1" step="0.01" value={values.illustrationFocusY} onChange={set('illustrationFocusY')} />
            </AdminField>
            {(fieldErrors.illustrationUrl || fieldErrors.illustrationThumbUrl) && (
              <small className="admin-field-error" role="alert">{fieldErrors.illustrationUrl ?? fieldErrors.illustrationThumbUrl}</small>
            )}
          </div>
        </div>

        <label className="admin-check is-wide">
          <input type="checkbox" checked={values.isActive} onChange={set('isActive')} />
          Attiva: esce dai pacchetti e si può usare nei mazzi
        </label>

        <div className="admin-form-actions is-wide">
          <button type="submit" className="admin-button admin-button--filled" disabled={saving}>
            {saving ? 'Salvataggio...' : isNew ? 'Crea la carta' : 'Salva'}
          </button>
          {!isNew && card.deckCount > 0 && <small className="collection-message">Usata in {card.deckCount} mazzi.</small>}
        </div>
      </form>
    </section>
  );
}
