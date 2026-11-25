import { useState } from 'react';
import { api, errorsByField } from '../../api.js';
import AdminField from './AdminField';
import ImageUpload from './ImageUpload';
import { factionNames, packTypeNames } from './adminFormat';

// I pesi di uno slot sono percentuali: la loro somma deve fare 100
const SLOT_TOTAL = 100;

// Data UTC del backend → valore di <input type="datetime-local"> nell'ora del browser, e ritorno
const toLocalInput = (iso) => {
  if (!iso) return '';
  const date = new Date(iso);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
const toIso = (local) => (local ? new Date(local).toISOString() : null);

// Il nome del pacchetto come codice delle immagini: "Dream Pack" → "dream-pack"
const slug = (text) => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
  .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40);

const newSlot = () => ({ cardCount: 1, rarities: [{ rarityCode: 'COMMON', weight: 100 }] });

function initialValues(pack) {
  return {
    name: pack?.name ?? '',
    description: pack?.description ?? '',
    packType: pack?.packType ?? 'SHOP',
    priceCredits: pack?.priceCredits ?? 500,
    cardSetCode: pack?.cardSetCode ?? '',
    factionCode: pack?.factionCode ?? '',
    fullArtChance: pack?.fullArtChance ?? 10,
    availableFrom: toLocalInput(pack?.availableFrom),
    availableUntil: toLocalInput(pack?.availableUntil),
    imageUrl: pack?.imageUrl ?? '',
    isActive: pack?.isActive ?? true,
    slots: pack?.slots?.map((slot) => ({ ...slot, rarities: slot.rarities.map((rarity) => ({ ...rarity })) })) ?? [newSlot()],
    covers: pack?.covers?.map((cover) => ({ ...cover, factionCode: cover.factionCode ?? '' })) ?? [],
  };
}

// Il corpo di POST e PUT /admin/packs: gli slot sostituiscono quelli di prima; le copertine con id si
// modificano, quelle senza id si aggiungono, quelle tolte dalla lista si spengono
function requestBody(values) {
  return {
    name: values.name.trim(),
    description: values.description.trim() || null,
    packType: values.packType,
    priceCredits: values.packType === 'SHOP' ? Number(values.priceCredits) : 0,
    cardSetCode: values.cardSetCode || null,
    factionCode: values.factionCode || null,
    fullArtChance: Number(values.fullArtChance),
    availableFrom: toIso(values.availableFrom),
    availableUntil: toIso(values.availableUntil),
    imageUrl: values.imageUrl || null,
    isActive: values.isActive,
    slots: values.slots.map((slot) => ({
      cardCount: Number(slot.cardCount),
      rarities: slot.rarities.map((rarity) => ({ rarityCode: rarity.rarityCode, weight: Number(rarity.weight) })),
    })),
    covers: values.covers.map((cover) => ({
      id: cover.id ?? null,
      name: cover.name.trim(),
      factionCode: cover.factionCode || null,
      imageUrl: cover.imageUrl,
      isActive: cover.isActive,
    })),
  };
}

// Scheda di un pacchetto del backoffice: nuovo pacchetto o modifica (POST/PUT /admin/packs).
// pack: il pacchetto da modificare, oppure null; options: { rarities, cardSets }
export default function PackEditor({ pack, options, onSaved, onClose }) {
  const isNew = !pack;
  const [values, setValues] = useState(() => initialValues(pack));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const fieldErrors = errorsByField(error);
  const imageCode = slug(values.name);

  const set = (name) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setValues((current) => ({ ...current, [name]: value }));
  };

  // Modifica di uno slot o di una copertina: si sostituisce l'elemento nella sua lista
  const updateSlot = (index, change) => setValues((current) => ({
    ...current,
    slots: current.slots.map((slot, position) => (position === index ? change(slot) : slot)),
  }));
  const updateCover = (index, change) => setValues((current) => ({
    ...current,
    covers: current.covers.map((cover, position) => (position === index ? { ...cover, ...change } : cover)),
  }));

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const body = requestBody(values);
      const saved = isNew
        ? await api('/admin/packs', { method: 'POST', body })
        : await api(`/admin/packs/${pack.id}`, { method: 'PUT', body });
      onSaved(saved, isNew ? 'Pacchetto creato.' : 'Pacchetto salvato.');
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="admin-panel admin-editor" aria-labelledby="pack-editor-title">
      <div className="admin-editor__head">
        <h2 id="pack-editor-title">{isNew ? 'Nuovo pacchetto' : pack.name}</h2>
        <button type="button" className="admin-button admin-button--small" onClick={onClose}>Chiudi</button>
      </div>

      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}

      <form className="admin-form" onSubmit={save} noValidate>
        <AdminField label="Nome" error={fieldErrors.name}>
          <input value={values.name} onChange={set('name')} maxLength={60} required aria-invalid={Boolean(fieldErrors.name)} />
        </AdminField>

        <AdminField label="Tipo" error={fieldErrors.packType} hint="Negozio: si compra. Set iniziale e Benvenuto: si regalano nel Benvenuto.">
          <select value={values.packType} onChange={set('packType')} aria-invalid={Boolean(fieldErrors.packType)}>
            {Object.entries(packTypeNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
          </select>
        </AdminField>

        <AdminField label="Descrizione" error={fieldErrors.description} wide>
          <textarea value={values.description} onChange={set('description')} maxLength={1000} aria-invalid={Boolean(fieldErrors.description)} />
        </AdminField>

        <AdminField label="Prezzo (DC)" error={fieldErrors.priceCredits} hint={values.packType === 'SHOP' ? 'Maggiore di 0.' : 'Gratis: si regala.'}>
          <input type="number" min="0" value={values.packType === 'SHOP' ? values.priceCredits : 0} onChange={set('priceCredits')} disabled={values.packType !== 'SHOP'} aria-invalid={Boolean(fieldErrors.priceCredits)} />
        </AdminField>

        <AdminField label="Probabilità full art (%)" error={fieldErrors.fullArtChance} hint="Quante Leggendarie escono in full art, da 0 a 100.">
          <input type="number" min="0" max="100" step="0.01" value={values.fullArtChance} onChange={set('fullArtChance')} aria-invalid={Boolean(fieldErrors.fullArtChance)} />
        </AdminField>

        <AdminField label="Solo carte di una fazione" error={fieldErrors.factionCode}>
          <select value={values.factionCode} onChange={set('factionCode')} aria-invalid={Boolean(fieldErrors.factionCode)}>
            <option value="">Tutte le fazioni</option>
            {Object.entries(factionNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
          </select>
        </AdminField>

        <AdminField label="Solo carte di un'espansione" error={fieldErrors.cardSetCode}>
          <select value={values.cardSetCode} onChange={set('cardSetCode')} aria-invalid={Boolean(fieldErrors.cardSetCode)}>
            <option value="">Tutte le espansioni</option>
            {options.cardSets.map((cardSet) => <option key={cardSet.code} value={cardSet.code}>{cardSet.name}</option>)}
          </select>
        </AdminField>

        <AdminField label="In vendita dal (facoltativo)" error={fieldErrors.availableFrom}>
          <input type="datetime-local" value={values.availableFrom} onChange={set('availableFrom')} aria-invalid={Boolean(fieldErrors.availableFrom)} />
        </AdminField>

        <AdminField label="In vendita fino al (facoltativo)" error={fieldErrors.availableUntil} hint="Dopo l'inizio.">
          <input type="datetime-local" value={values.availableUntil} onChange={set('availableUntil')} aria-invalid={Boolean(fieldErrors.availableUntil)} />
        </AdminField>

        <div className="admin-artwork is-wide">
          <div className="admin-artwork__preview admin-artwork__preview--tall">
            {values.imageUrl ? <img src={values.imageUrl} alt="Immagine del pacchetto" /> : <span>Nessuna immagine</span>}
          </div>
          <div className="admin-artwork__controls">
            <ImageUpload
              code={imageCode}
              type="COVER"
              label={values.imageUrl ? "Cambia l'immagine del pacchetto" : "Carica l'immagine del pacchetto"}
              onUploaded={({ url }) => setValues((current) => ({ ...current, imageUrl: url }))}
            />
            {values.imageUrl && (
              <button type="button" className="admin-button admin-button--small admin-upload" onClick={() => setValues((current) => ({ ...current, imageUrl: '' }))}>Togli l'immagine</button>
            )}
          </div>
        </div>

        <div className="is-wide">
          <h3>Slot</h3>
          <p className="admin-panel__intro">Ogni slot dà un certo numero di carte; le probabilità delle rarità di uno slot sommano a 100.</p>
          {fieldErrors.slots && <p className="admin-field-error" role="alert">{fieldErrors.slots}</p>}
          {values.slots.map((slot, index) => (
            <SlotEditor
              key={index}
              slot={slot}
              index={index}
              rarities={options.rarities}
              canRemove={values.slots.length > 1}
              onChange={(change) => updateSlot(index, change)}
              onRemove={() => setValues((current) => ({ ...current, slots: current.slots.filter((_, position) => position !== index) }))}
            />
          ))}
          <button type="button" className="admin-button admin-button--small" onClick={() => setValues((current) => ({ ...current, slots: [...current.slots, newSlot()] }))}>Aggiungi uno slot</button>
        </div>

        <div className="is-wide">
          <h3>Copertine</h3>
          <p className="admin-panel__intro">Le varianti grafiche del pacchetto. Una copertina tolta dalla lista si spegne: le aperture già fatte la ricordano.</p>
          {fieldErrors.covers && <p className="admin-field-error" role="alert">{fieldErrors.covers}</p>}
          {values.covers.map((cover, index) => (
            <div key={cover.id ?? `new-${index}`} className="admin-slot">
              <div className="admin-form admin-form--three">
                <AdminField label="Nome della copertina">
                  <input value={cover.name} onChange={(event) => updateCover(index, { name: event.target.value })} maxLength={60} required />
                </AdminField>
                <AdminField label="Fazione del Lord">
                  <select value={cover.factionCode} onChange={(event) => updateCover(index, { factionCode: event.target.value })}>
                    <option value="">Nessuna</option>
                    {Object.entries(factionNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
                  </select>
                </AdminField>
                <label className="admin-check">
                  <input type="checkbox" checked={cover.isActive} onChange={(event) => updateCover(index, { isActive: event.target.checked })} />
                  Attiva
                </label>
              </div>
              <div className="admin-artwork">
                <div className="admin-artwork__preview admin-artwork__preview--tall">
                  {cover.imageUrl ? <img src={cover.imageUrl} alt={`Copertina ${cover.name}`} /> : <span>Nessuna immagine</span>}
                </div>
                <div className="admin-artwork__controls">
                  <ImageUpload
                    code={slug(`${values.name} ${cover.name}`)}
                    type="COVER"
                    label={cover.imageUrl ? 'Cambia la copertina' : 'Carica la copertina'}
                    onUploaded={({ url }) => updateCover(index, { imageUrl: url })}
                  />
                  <button type="button" className="admin-button admin-button--small admin-button--danger admin-upload" onClick={() => setValues((current) => ({ ...current, covers: current.covers.filter((_, position) => position !== index) }))}>
                    Togli dalla lista
                  </button>
                </div>
              </div>
            </div>
          ))}
          <button type="button" className="admin-button admin-button--small" onClick={() => setValues((current) => ({ ...current, covers: [...current.covers, { id: null, name: '', factionCode: '', imageUrl: '', isActive: true }] }))}>
            Aggiungi una copertina
          </button>
        </div>

        <label className="admin-check is-wide">
          <input type="checkbox" checked={values.isActive} onChange={set('isActive')} />
          Attivo: si vende (o si regala) nel periodo scelto
        </label>

        <div className="admin-form-actions is-wide">
          <button type="submit" className="admin-button admin-button--filled" disabled={saving}>
            {saving ? 'Salvataggio...' : isNew ? 'Crea il pacchetto' : 'Salva'}
          </button>
        </div>
      </form>
    </section>
  );
}

// Uno slot: quante carte dà e con quali rarità, con il totale delle probabilità in diretta
function SlotEditor({ slot, index, rarities, canRemove, onChange, onRemove }) {
  const total = slot.rarities.reduce((sum, rarity) => sum + (Number(rarity.weight) || 0), 0);
  const unused = rarities.find((rarity) => !slot.rarities.some((item) => item.rarityCode === rarity.code));

  const setRarity = (position, change) => onChange((current) => ({
    ...current,
    rarities: current.rarities.map((rarity, item) => (item === position ? { ...rarity, ...change } : rarity)),
  }));

  return (
    <div className="admin-slot">
      <div className="admin-slot__head">
        <strong>Slot {index + 1}</strong>
        <label className="admin-check">
          Carte
          <input type="number" min="1" max="20" value={slot.cardCount} onChange={(event) => onChange((current) => ({ ...current, cardCount: event.target.value }))} style={{ width: 70 }} />
        </label>
        <span className={`admin-slot__total${total === SLOT_TOTAL ? '' : ' is-wrong'}`}>Totale {total}%{total === SLOT_TOTAL ? '' : ' (deve fare 100)'}</span>
        {canRemove && <button type="button" className="admin-button admin-button--small admin-button--danger" onClick={onRemove}>Togli</button>}
      </div>
      {slot.rarities.map((rarity, position) => (
        <div key={position} className="admin-slot__rarity">
          <select value={rarity.rarityCode} onChange={(event) => setRarity(position, { rarityCode: event.target.value })} aria-label={`Rarità ${position + 1} dello slot ${index + 1}`}>
            {rarities.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}
          </select>
          <input type="number" min="1" max="100" value={rarity.weight} onChange={(event) => setRarity(position, { weight: event.target.value })} aria-label={`Probabilità in percentuale della rarità ${position + 1}`} />
          {slot.rarities.length > 1 ? (
            <button type="button" className="admin-button admin-button--small" onClick={() => onChange((current) => ({ ...current, rarities: current.rarities.filter((_, item) => item !== position) }))}>Togli</button>
          ) : <span />}
        </div>
      ))}
      {unused && (
        <button type="button" className="admin-button admin-button--small" onClick={() => onChange((current) => ({ ...current, rarities: [...current.rarities, { rarityCode: unused.code, weight: 0 }] }))}>
          Aggiungi una rarità
        </button>
      )}
    </div>
  );
}
