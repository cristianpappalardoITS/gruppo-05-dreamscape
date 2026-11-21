-- V104__set_shop_pack_images.sql
-- Le immagini dei sei pacchetti del negozio (#91, decisione 030), consegnate da Digital il 21/11:
-- Dream Pack con il retro delle carte (proposta 3, il monolite sul lago), i tre pacchetti di
-- fazione con il loro Lord, Nap Pack e REM Pack con due proposte nuove ancora da approvare.
-- Le copie WebP (1024 px, sfondo trasparente) sono in src/main/resources/images/packs/ e
-- arrivano con il backend, come le illustrazioni del catalogo. Stesso nome che darebbe il
-- backoffice (nome del pacchetto e versione): un'immagine nuova sarà la v2, caricata dal
-- backoffice o con una migration nuova, mai modificando questa.

UPDATE packs p
SET image_url = v.image_url
FROM (VALUES
    ('Nap Pack',        '/images/packs/nap-pack-v1.webp'),
    ('Dream Pack',      '/images/packs/dream-pack-v1.webp'),
    ('Lucid Pack',      '/images/packs/lucid-pack-v1.webp'),
    ('Faceless Pack',   '/images/packs/faceless-pack-v1.webp'),
    ('Still Time Pack', '/images/packs/still-time-pack-v1.webp'),
    ('REM Pack',        '/images/packs/rem-pack-v1.webp')
) AS v (name, image_url)
WHERE p.name = v.name
  AND p.pack_type = 'SHOP';
