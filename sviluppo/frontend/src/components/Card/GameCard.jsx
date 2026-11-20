import { useEffect, useState } from "react";
import GameCardHeader from "./GameCardHeader.jsx";
import GameCardArtwork from "./GameCardArtwork.jsx";
import GameCardDescriptionBox from "./GameCardDescriptionBox.jsx";
import GameCardFooter from "./GameCardFooter.jsx";
import { getCardFrame, getCardLayout } from "./cardAssets.js";
import "@fontsource/cinzel/600.css";
import "@fontsource/cormorant-garamond/500.css";
import "@fontsource/cormorant-garamond/700.css";
import "./game-card.css";

const keyedFrames = new Map();

function createKeyedFrame(source) {
  if (!keyedFrames.has(source)) {
    keyedFrames.set(
      source,
      new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => {
          try {
            const canvas = document.createElement("canvas");
            canvas.width = image.naturalWidth;
            canvas.height = image.naturalHeight;

            const context = canvas.getContext("2d", {
              willReadFrequently: true,
            });
            if (!context) {
              throw new Error("Impossibile inizializzare il canvas della cornice.");
            }

            context.drawImage(image, 0, 0);
            const frame = context.getImageData(
              0,
              0,
              canvas.width,
              canvas.height,
            );

            for (let index = 0; index < frame.data.length; index += 4) {
              const red = frame.data[index];
              const green = frame.data[index + 1];
              const blue = frame.data[index + 2];
              const greenDominance = green - Math.max(red, blue);

              if (green > 70 && greenDominance > 8) {
                const keyedAmount = Math.min(1, (greenDominance - 8) / 55);
                frame.data[index + 1] = Math.round(
                  green + (Math.max(red, blue) - green) * keyedAmount,
                );
                frame.data[index + 3] = Math.round(
                  frame.data[index + 3] * (1 - keyedAmount),
                );
              }
            }

            context.putImageData(frame, 0, 0);
            resolve(canvas.toDataURL("image/png"));
          } catch (error) {
            reject(error);
          }
        };
        image.onerror = () =>
          reject(new Error(`Impossibile caricare la cornice: ${source}`));
        image.src = source;
      }),
    );
  }

  return keyedFrames.get(source);
}

function boxStyle([left, top, width, height]) {
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${width}%`,
    height: `${height}%`,
  };
}

function pointStyle([left, top]) {
  return { left: `${left}%`, top: `${top}%` };
}

export default function GameCard({ card, size = "thumb" }) {
  const isFullArt = card.variantType === "FULL_ART";
  const frame = isFullArt ? null : getCardFrame(card);
  const layout = getCardLayout(card);
  const artUrl =
    size === "large"
      ? card.illustrationUrl ?? card.illustrationThumbUrl
      : card.illustrationThumbUrl ?? card.illustrationUrl;
  const [keyedFrame, setKeyedFrame] = useState(null);

  useEffect(() => {
    let active = true;

    if (frame) {
      createKeyedFrame(frame)
        .then((transparentFrame) => {
          if (active) {
            setKeyedFrame({ source: frame, url: transparentFrame });
          }
        })
        .catch((error) => {
          console.error("El rendering della cornice della carta è fallito.", error);
        });
    }

    return () => {
      active = false;
    };
  }, [frame]);

  const frameUrl = keyedFrame?.source === frame ? keyedFrame.url : null;

  if (!isFullArt && !frame) {
    throw new Error(
      `Cornice non trovata per ${card.faction?.code}/${card.rarity?.code}.`,
    );
  }

  const label = [
    card.name,
    card.faction?.name,
    card.rarity?.name,
    `attacco ${card.attack}`,
    `difesa ${card.defense}`,
  ]
    .filter(Boolean)
    .join(", ");
  const descriptionStyle = isFullArt
    ? boxStyle([8, 62, 84, 9])
    : boxStyle(layout.description);
  const effectStyle = isFullArt
    ? boxStyle([8, 73, 84, 12])
    : boxStyle(layout.effect);
  const nameStyle = isFullArt
    ? boxStyle([8, 5, 84, 10])
    : boxStyle(layout.name);
  const attackStyle = isFullArt
    ? pointStyle([23, 91])
    : pointStyle(layout.attack);
  const defenseStyle = isFullArt
    ? pointStyle([77, 91])
    : pointStyle(layout.defense);
  const effect = card.effect;

  return (
    <figure
      className={`gcard gcard--${size}${isFullArt ? " gcard--full-art" : ""}`}
      role="img"
      aria-label={label}
    >
      <GameCardArtwork
        imageUrl={artUrl}
        name={card.name}
        objectPosition={`50% ${Math.max(
          0,
          Math.min(1, card.illustrationFocusY ?? 0.4),
        ) * 100}%`}
        style={isFullArt ? { inset: 0 } : boxStyle(layout.artwork)}
      />
      {frameUrl && (
        <img className="gcard__frame" src={frameUrl} alt="" aria-hidden="true" />
      )}
      <GameCardHeader
        name={card.name}
        lordTitle={card.lordTitle}
        style={nameStyle}
      />
      {card.description && (
        <GameCardDescriptionBox
          className="gcard__text gcard__description"
          content={card.description}
          style={descriptionStyle}
        />
      )}
      {effect && (
        <GameCardDescriptionBox
          className="gcard__text gcard__effect"
          content={
            <>
              <strong>
                {effect.name}
                {effect.value != null ? ` ${effect.value}` : ""}:
              </strong>{" "}
              {effect.text}
            </>
          }
          style={effectStyle}
        />
      )}
      <GameCardFooter
        attack={card.attack}
        defense={card.defense}
        attackStyle={attackStyle}
        defenseStyle={defenseStyle}
      />
    </figure>
  );
}
