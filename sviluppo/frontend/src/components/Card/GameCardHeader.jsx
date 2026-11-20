export default function GameCardHeader({ name, lordTitle, style }) {
  return (
    <h3 className="gcard__name" style={style}>
      {lordTitle && <span className="gcard__lord">{lordTitle}</span>}
      {name}
    </h3>
  );
}
