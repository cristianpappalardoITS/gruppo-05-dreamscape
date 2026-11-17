# 032 · Accesso con Google

- **Data:** 2026-11-17
- **Stato:** approvata
- **Referenti:** Cristian
- **Task:** #159, #73

## Contesto
La pagina di accesso disegnata da WMD (#73) ha il pulsante "Continua con Google", che il backend non prevedeva: la [003](003-accesso-jwt.md) parla solo di username o email e password. Il gruppo ha deciso di aggiungerlo. Restavano da scegliere il giro tra frontend e backend e cosa fare con chi non ha ancora un account o ne ha già uno con la stessa email.

## Opzioni valutate
1. **Giro:** il frontend usa il pulsante ufficiale di Google e manda al backend l'id token che riceve; oppure il backend fa tutto il giro OAuth2 con i redirect verso Google.
2. **Primo accesso senza account:** un piccolo form con username e consensi; oppure account creato da solo, con l'username preso dall'email.
3. **Email già registrata con la password:** collegare l'account a Google; oppure rifiutare e far entrare con la password.

## Decisione
- **Id token al backend.** Il frontend usa Google Identity Services e manda l'id token a `POST /auth/google`. Il backend controlla firma (chiavi pubbliche di Google), scadenza, emittente (`accounts.google.com`) e destinatario (il nostro client id), poi risponde come `POST /auth/login`: stesso token di accesso di 15 minuti, stesso refresh token nel cookie. La 003 non cambia.
- **Si riconosce l'account Google dal suo id (`sub`), non dall'email**, che su Google può cambiare: `users.google_subject`, unico (`V11`).
- **Primo accesso senza account:** risposta `202` con email e username proposto; il frontend mostra un piccolo form (username, Privacy, Termini) e rimanda la richiesta; l'account nasce con `201`, con il bonus di registrazione, ed è già dentro. I consensi sono obbligatori come nella registrazione normale ([018](018-utenti-e-privacy.md)).
- **Email già registrata:** se Google dice che l'email è verificata, l'account esistente si collega a Google e da lì si entra in tutti e due i modi. Senza email verificata non si collega e non si crea niente. Un account già collegato a un altro account Google non cambia (`409 GOOGLE_ACCOUNT_CONFLICT`).
- **Account senza password:** chi si registra con Google ha `password_hash` a `NULL`. Accesso con la password, cambio password e cancellazione dell'account (che chiedono la password) non funzionano finché non sceglie una password con "Password dimenticata" (`400 PASSWORD_NOT_SET`). Almeno un modo di accesso deve sempre esserci (`chk_users_login_method`).
- **Cancellazione dell'account:** come prima, più `google_subject` torna `NULL`, così con lo stesso account Google ci si può registrare di nuovo.
- **Configurazione:** il client id arriva da `GOOGLE_CLIENT_ID`, mai nel repository. Senza, l'accesso con Google è spento (`503 GOOGLE_LOGIN_DISABLED`) e il frontend nasconde il pulsante. Nessuna libreria nuova: la verifica usa Spring Security, già nel backend.

## Motivazione
Con l'id token il backend resta senza stato e il frontend riceve il token come con la password: niente redirect da gestire e nessuna sessione. Il form al primo accesso tiene le stesse regole della registrazione normale (username scelto dalla persona, consensi espliciti). Collegare l'account esistente evita due account per la stessa persona; farlo solo con l'email verificata da Google impedisce di prendersi l'account di un altro.

## Conseguenze
- Migration `V11`: `users.google_subject` e `password_hash` facoltativa. Nel database di sviluppo con i dati finti serve `docker compose down -v` ([README del backend](../../sviluppo/backend/README.md#dopo-una-migration-nuova)).
- [Guida per il frontend](../api/guida-frontend.md#accesso-con-google) e [elenco degli endpoint](../api/endpoint-e-fetch.md) con `POST /auth/google`; [schema.md](../database/schema.md) con le colonne nuove.
- Il progetto e il client OAuth su Google Cloud li crea Cristian; il client id va dato a WMD per il pulsante (#73).
- Da dire a Alessandro (#73): pulsante ufficiale di Google, form del `202`, pulsante nascosto con `503`.
