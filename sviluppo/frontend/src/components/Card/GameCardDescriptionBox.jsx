export default function GameCardDescriptionBox({
  content,
  className,
  style,
}) {
  return (
    <p className={className} style={style}>
      {content}
    </p>
  );
}
