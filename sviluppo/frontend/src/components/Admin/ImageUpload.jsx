import { useState } from 'react';
import { api } from '../../api.js';

// Carica un'immagine originale (PNG o JPEG) con POST /admin/images: il backend la conserva e crea le copie
// WebP, poi onUploaded riceve { url, thumbUrl } (le copertine hanno solo url). Il caricamento non cambia
// ancora la carta o il pacchetto: gli indirizzi si salvano con il form.
// type: ILLUSTRATION, FULL_ART o COVER; code: codice della carta o nome del pacchetto in minuscolo
export default function ImageUpload({ code, type, label = 'Carica immagine', onUploaded }) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);

  const upload = async (event) => {
    const file = event.target.files[0];
    event.target.value = ''; // si può ricaricare lo stesso file
    if (!file) return;
    setError(null);
    setUploading(true);
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('code', code);
      form.append('type', type);
      onUploaded(await api('/admin/images', { method: 'POST', body: form }));
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="admin-field">
      <label className={`admin-button admin-button--small admin-upload${!code || uploading ? ' is-disabled' : ''}`}>
        {uploading ? 'Caricamento...' : label}
        <input type="file" accept="image/png,image/jpeg" className="visually-hidden" onChange={upload} disabled={!code || uploading} />
      </label>
      {!code && <small>Scrivi prima il codice.</small>}
      {code && !error && <small>PNG o JPEG, al massimo 15 MB.</small>}
      {error && <small className="field-error" role="alert">{error}</small>}
    </div>
  );
}
