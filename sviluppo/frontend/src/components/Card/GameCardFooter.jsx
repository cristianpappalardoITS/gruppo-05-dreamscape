export default function GameCardFooter({
  attack,
  defense,
  attackStyle,
  defenseStyle,
}) {
  return (
    <>
      <span className="gcard__stat gcard__stat--attack" style={attackStyle}>
        {attack}
      </span>
      <span className="gcard__stat gcard__stat--defense" style={defenseStyle}>
        {defense}
      </span>
    </>
  );
}
