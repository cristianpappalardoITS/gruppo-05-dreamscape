import AdminLayout from '../components/Admin/AdminLayout';

// Messaggi del form Contatti: le API arrivano con il form (#162), poi qui si leggono e si archiviano
export default function AdminMessagesPage() {
  return (
    <AdminLayout title="Messaggi">
      <p className="admin-empty">I messaggi del form Contatti arriveranno qui quando il form sarà pronto (#162).</p>
    </AdminLayout>
  );
}
