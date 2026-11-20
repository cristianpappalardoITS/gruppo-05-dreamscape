import { Logo, Cart, UserIcon } from "./ProfileIcons.jsx";

const links = ["Collezione", "Mazzi", "Scambio", "Arena"];

export default function ProfileNavbar() {
  return (
    <header className="navbar">
      <a href="#" className="navbar-logo">
        <Logo />
      </a>

      <nav className="navbar-links">
        {links.map((label) => (
          <a key={label} href="#">
            {label}
          </a>
        ))}
      </nav>

      <div className="navbar-icons">
        <a href="#" aria-label="Carrello">
          <Cart />
        </a>
        <a href="#" aria-label="Profilo">
          <UserIcon />
        </a>
      </div>
    </header>
  );
}
