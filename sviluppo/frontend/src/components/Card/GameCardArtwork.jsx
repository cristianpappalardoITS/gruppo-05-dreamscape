export default function GameCardArtwork({
  imageUrl,
  name,
  objectPosition,
  style,
}) {
  if (!imageUrl) {
    return (
      <div
        className="gcard__art gcard__art--missing"
        role="img"
        aria-label={`Illustrazione non disponibile: ${name}`}
        style={style}
      />
    );
  }

  return (
    <img
      className="gcard__art"
      src={imageUrl}
      alt=""
      loading="lazy"
      style={{ ...style, objectPosition }}
    />
  );
}
