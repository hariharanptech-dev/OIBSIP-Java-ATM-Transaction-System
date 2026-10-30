# ATM Interface

A premium terminal ATM simulator in object-oriented Java 17+. The banking logic is classic OOP
(`Bank`, `Account`, `Transaction`); the focus is a polished console experience: bordered panels,
Indian-grouped rupee formatting, masked PIN entry, confirmation and receipt screens, and input
validation that never crashes.

No runtime libraries. JUnit 5 is used for tests only.

## Prerequisites

- JDK 17 or newer (`java -version`)
- Maven 3.8+ (optional, only for `mvn` builds)
- A UTF-8 capable terminal for the default look. On Windows CMD run `chcp 65001` first, or use
  `--ascii`. Windows Terminal, macOS Terminal and most Linux terminals work as-is.

## Build and run

### Maven

```bash
mvn clean package              # compiles, runs the unit tests, builds target/atm-interface.jar
java -jar target/atm-interface.jar
java -jar target/atm-interface.jar --ascii --no-color
mvn test                       # tests only
```

### Plain javac

```bash
# macOS / Linux / Git Bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main -name "*.java")
java -cp out com.atm.Main
```

```powershell
# Windows PowerShell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src/main -Filter *.java).FullName
java -cp out com.atm.Main
```

To run the tests without Maven, download `junit-platform-console-standalone` (5.10.x) from Maven
Central and run:

```bash
javac -encoding UTF-8 -cp out:junit-platform-console-standalone.jar -d test-out $(find src/test -name "*.java")
java -jar junit-platform-console-standalone.jar --class-path out:test-out --scan-class-path
```
(use `;` instead of `:` in the class path on Windows)

> For the best experience run in a real terminal. IDE consoles usually cannot mask the PIN or
> clear the screen; the app detects this, prints a one-line notice and falls back gracefully.

## Command-line flags

| Flag | Effect |
|------|--------|
| `--no-color` | No ANSI escape codes at all, and no screen clearing |
| `--color` | Force colours (for IDE consoles that support ANSI but are not detected) |
| `--ascii` | Use `+ - \| =` borders, `Rs.` and text markers `[H] [W] [D] [T] [Q]` instead of Unicode |
| `--fast` | Skip the start-up progress bar |
| `--relogin` | After Quit return to the login screen (kiosk mode) so you can log in as the transfer recipient and see their history |
| `--help` | Print usage |

Colour is also disabled automatically when output is piped, `NO_COLOR` is set, or `TERM=dumb`.
Unknown flags print usage and exit with code 2.

## Demo credentials

| User ID | PIN | Holder | Opening balance |
|---------|------|--------|-----------------|
| 1001 | 1234 | Aarav Kumar | ₹25,000.00 |
| 1002 | 4321 | Priya Sharma | ₹48,500.00 |
| 1003 | 1111 | Rahul Verma | ₹10,000.00 |
| 1004 | 2222 | Sneha Iyer | ₹75,250.50 |

Data lives in memory only; every run starts from these balances.

## Rules at a glance

- 3 incorrect login attempts, then "Access Denied: too many incorrect attempts" and exit (code 1).
- Max ₹1,00,000.00 per transaction (deposit, withdrawal, transfer). Withdrawals in multiples of ₹100.
- Amounts: positive, at most 2 decimals. Type `0` or `b` at any amount / recipient / quick-select prompt to cancel.
- Withdrawals and transfers show a "You are about to..." panel and ask `Confirm? (Y/N)`.
- Every successful transaction prints a dashed-border receipt that stays on screen until Enter.

## Feature checklist

- [x] Login with User ID + PIN, 3-attempt lock-out, attempts indicator (`●●○`), lock screen
- [x] Masked PIN via `System.console().readPassword()`; visible-input notice when no console
- [x] Main menu card: time-of-day greeting, masked account (`XXXX1001`), balance, icons, footer hint
- [x] Transaction history table (ID, date/time, type, amount, balance after, details), totals, empty state
- [x] Withdraw with quick-select amounts, limit, multiple-of-100, balance check, confirmation, receipt
- [x] Deposit with limit, receipt
- [x] Transfer with recipient validation, self-transfer block, balance check, confirmation, atomic update, TRANSFER_OUT / TRANSFER_IN on both accounts
- [x] Quit with goodbye screen and session summary
- [x] Balance shown on the menu and on every receipt
- [x] Indian digit grouping, `Rs.` in ASCII mode, UTF-8 output stream
- [x] Start-up banner and progress bar (about 1.2 s, skipped with `--fast`)
- [x] ANSI auto-detection, `--no-color`, `--ascii`, screen clearing only on a real terminal
- [x] Never crashes on bad input; EOF (Ctrl+D / Ctrl+Z) handled
- [x] JUnit 5 tests, TESTING.md (49 manual cases), SAMPLE_SESSION.txt

## Class diagram (text)

```
com.atm
  Main ───────────────► seeds Bank, parses flags, builds UI, starts ATM
  ATM  ───────────────► Bank, ConsoleUI, InputHelper, LoginAttemptTracker
  Bank ◆───────────────► Map<String, Account>, TransactionIdGenerator
  Account ◆────────────► List<Transaction> (ArrayList), BigDecimal balance, SHA-256 pinHash
  Transaction (immutable) ── TransactionType (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
  Money, SessionSummary (record), TransferReceipt (record), LoginAttemptTracker
  AtmException (unchecked base)
    ├─ InvalidAmountException (Reason enum)   ├─ InsufficientFundsException
    ├─ AccountNotFoundException               ├─ AuthenticationException
    └─ InvalidTransferException               └─ TransferFailedException

com.atm.ui
  ConsoleUI ───────────► Theme, CurrencyFormatter       (all printing)
  InputHelper ─────────► ConsoleUI                       (all reading and validation)
  Theme                                                  (ONLY place with ANSI codes, symbols, box characters)
  CurrencyFormatter, InputClosedException
```

Dependency direction: `ui` knows `Account`/`Transaction` only to render them; `Bank` and `Account` never
import anything from `ui`, and contain no `System.out` calls.

## Design decisions

- **BigDecimal everywhere** at scale 2, HALF_UP; amounts with more than 2 real decimals are rejected, not silently rounded.
- **PIN hashing**: SHA-256 over `accountId:pin` (the id acts as a per-account salt). Verification uses
  `MessageDigest.isEqual` (constant time). A production system would use a slow KDF such as bcrypt/PBKDF2; SHA-256 is what the brief asks for.
- **Same message for unknown user and wrong PIN** so ids cannot be enumerated. PIN format errors (not 4 digits) do not consume an attempt.
- **Exceptions carry a reason, the UI words it**: `InvalidAmountException.Reason` lets the UI say "multiples of ₹100" with proper currency formatting while the domain stays free of presentation.
- **Atomic transfer**: debit, then credit; if the credit throws, the debit is undone (balance and history entry) and `TransferFailedException` is raised. A rolled-back transfer consumes one transaction id, so ids are unique but may have a gap.
- **Session history**: there is one login per run, so "current session" equals the account's in-memory history. Incoming transfers appear as TRANSFER_IN for the recipient (use `--relogin` to see it).
- **Display width**: panel padding measures terminal columns (ANSI stripped, emoji counted as two) so borders stay aligned.
- **Unicode in source** is written as `\u` escapes and confined to `Theme`, so plain `javac` works on any default locale.
- **Interactive detection**: animation and screen clearing run only on a real terminal. On JDK 22+ `System.console()` is non-null even when piped, so `Console.isTerminal()` is consulted by reflection to stay compatible with JDK 17.
- **Not implemented on purpose**: persistence, inactivity timeout (optional in the brief; the footer says "Session secured"), per-account lock-out across restarts.
