import { useEffect, useState } from 'react';
import { api } from '../api.js';
import AdminLayout from '../components/Admin/AdminLayout';
import PackEditor from '../components/Admin/PackEditor';
import { factionNames, formatNumber, packTypeNames } from '../components/Admin/adminFormat';

// Un pacchetto è in vendita adesso se è attivo e dentro il suo periodo (stesse regole del negozio)
function isOnSale(pack, now = Date.now()) {
  if (!pack.isActive) return false;
  if (pack.availableFrom && new Date(pack.availableFrom).getTime() > now) return false;
  return !(pack.availableUntil && new Date(pack.availableUntil).getTime() <= now);
}

const cardCount = (pack) => pack.slots.reduce((sum, slot) => sum + slot.cardCount, 0);

// Pacchetti del backoffice (GET /admin/packs): tutti, anche spenti, set iniziali e benvenuto.
// I pacchetti non si cancellano: si spengono. A destra la scheda per crearli e modificarli (PackEditor).
export default function AdminPacksPage() {
  const [packs, setPacks] = useState(null);
  const [options, setOptions] = useState(null);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(null);
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    api('/admin/packs').then(setPacks).catch(setError);
    Promise.all([api('/admin/rarities'), api('/admin/card-sets')])
      .then(([rarities, cardSets]) => setOptions({ rarities, cardSets }))
      .catch(setError);
  }, []);

  const saved = async (pack, message) => {
    setNotice(message);
    setEditing({ pack });
    try {
      setPacks(await api('/admin/packs'));
    } catch (err) {
      setError(err);
    }
  };

  const open = (state) => {
    setNotice(null);
    setEditing(state);
  };

  const actions = (
    <button type="button" className="admin-button admin-button--filled" onClick={() => open({ pack: null })} disabled={!options}>Nuovo pacchetto</button>
  );

  return (
    <AdminLayout title="Pacchetti" actions={actions}>
      {error && <p className="admin-message admin-message--error" role="alert">{error.message}</p>}
      {notice && <p className="admin-message admin-message--ok" role="status">{notice}</p>}

      <div className={editing ? 'admin-columns' : undefined}>
        <section aria-label="Elenco dei pacchetti">
          {!packs && !error && <p className="collection-message">Caricamento dei pacchetti...</p>}
          {packs && packs.length === 0 && <p className="admin-empty">Non ci sono pacchetti.</p>}
          {packs && packs.length > 0 && (
            <div className="admin-table-wrapper">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th scope="col"><span className="visually-hidden">Immagine</span></th>
                    <th scope="col">Nome</th>
                    <th scope="col" className="admin-optional">Tipo</th>
                    <th scope="col" className="admin-number">Prezzo</th>
                    <th scope="col" className="admin-number">Carte</th>
                    <th scope="col" className="admin-optional">Fazione</th>
                    <th scope="col">Stato</th>
                    <th scope="col"><span className="visually-hidden">Azioni</span></th>
                  </tr>
                </thead>
                <tbody>
                  {packs.map((pack) => {
                    const onSale = isOnSale(pack);
                    return (
                      <tr key={pack.id} className={`${pack.id === editing?.pack?.id ? 'is-selected' : ''}${pack.isActive ? '' : ' is-off'}`}>
                        <td>{pack.imageUrl ? <img className="admin-thumb" src={pack.imageUrl} alt="" /> : <span className="admin-thumb" />}</td>
                        <td>{pack.name}</td>
                        <td className="admin-optional">{packTypeNames[pack.packType] ?? pack.packType}</td>
                        <td className="admin-number">{pack.priceCredits ? `${formatNumber(pack.priceCredits)} DC` : 'Gratis'}</td>
                        <td className="admin-number">{cardCount(pack)}</td>
                        <td className="admin-optional">{factionNames[pack.factionCode] ?? 'Tutte'}</td>
                        <td>
                          <span className={`admin-status ${onSale ? 'admin-status--on' : 'admin-status--off'}`}>
                            {!pack.isActive ? 'Spento' : onSale ? 'Attivo' : 'Fuori periodo'}
                          </span>
                        </td>
                        <td>
                          <button type="button" className="admin-button admin-button--small" onClick={() => open({ pack })} disabled={!options}>Modifica</button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {editing && options && (
          <PackEditor
            key={editing.pack?.id ?? 'new-pack'}
            pack={editing.pack}
            options={options}
            onSaved={saved}
            onClose={() => setEditing(null)}
          />
        )}
      </div>
    </AdminLayout>
  );
}
