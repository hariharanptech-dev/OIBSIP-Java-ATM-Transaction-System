# TESTING.md - Manual test cases

Run the app with `java -cp out com.atm.Main` (or `java -jar target/atm-interface.jar`). Demo users: `1001/1234` (₹25,000.00), `1002/4321` (₹48,500.00), `1003/1111` (₹10,000.00), `1004/2222` (₹75,250.50). Balances reset on every run.

Use `--relogin` when a case needs a second user in the same run. Add `--fast` to skip the animation when repeating cases.

| # | Area | Case | Steps | Expected result |
|---|------|------|-------|-----------------|
| TC-01 | Login | Valid login | Start app, User ID `1001`, PIN `1234` | Green success line, then MAIN MENU: greeting matching time of day, `XXXX1001`, balance ₹25,000.00 |
| TC-02 | Login | Wrong PIN once | User ID `1001`, PIN `0000` | Red `✖ Incorrect User ID or PIN.` and `Attempts left: ●●○ (2 of 3)`; login prompt returns |
| TC-03 | Login | Unknown User ID | User ID `9999`, PIN `1234` | Identical message to wrong PIN (does not reveal whether the ID exists); attempt counted |
| TC-04 | Login | Three failures lock the terminal | Enter wrong credentials three times | Indicators ●●○, ●○○, ○○○; then red ACCESS DENIED panel with `Access Denied: too many incorrect attempts`; program exits with code 1 (`echo $?` / `echo %ERRORLEVEL%`) |
| TC-05 | Login | Success after failures | One wrong attempt, then `1001` / `1234` | Login succeeds; no lock-out |
| TC-06 | Login | Empty User ID | Press Enter at User ID | Inline error `Please enter your User ID.`; re-prompt; attempt NOT consumed |
| TC-07 | Login | Malformed PIN | PIN `12`, `abcd`, `12345` | `PIN must be exactly 4 digits.`; re-prompt; attempt NOT consumed |
| TC-08 | Login | PIN masked on a real terminal | Run in Terminal/PowerShell, type a PIN | Typed digits are not echoed; no visible-input notice is shown |
| TC-09 | Login | PIN note in IDE / piped input | Run from an IDE console or `echo ... | java ...` | Yellow note `this console cannot hide input, so your PIN will be visible.` appears once; login still works |
| TC-10 | Menu | Non-numeric choice | At `Select an option` type `abc` | `Please enter a number between 1 and 5.`; re-prompt; no crash |
| TC-11 | Menu | Out-of-range and empty choice | Type `0`, `6`, `-1`, then just Enter | Range message for the first three; `Please make a selection (1-5).` for empty |
| TC-12 | Menu | Menu content | Log in as `1001` | Card shows greeting, date/time, `XXXX1001`, balance, five numbered items with icons (`[H] [W] [D] [T] [Q]` with `--ascii`), hint `Enter 1-5`, `Session secured` |
| TC-13 | History | Empty state | Log in, choose 1 | Friendly `No transactions yet in this session.` message; Enter returns to the menu |
| TC-14 | History | After several transactions | Deposit 1000, withdraw 500, transfer 200 to 1002, choose 1 | Table lists 3 rows oldest first with Txn ID (TXN-000001..), date/time, type, amount (green `+`, red `-`), balance after, detail line; totals footer shows Deposited, Withdrawn, Transferred out/in, count and net change; rows alternate shading in colour mode |
| TC-15 | Withdraw | Quick amount with confirmation | Choose 2, quick option 2, confirm `y` | Confirmation panel `You are about to withdraw ₹1,000.00`; receipt (Txn ID, date/time, Withdrawal, amount, new balance ₹24,000.00, `XXXX1001`, `Thank you for banking with us`); receipt stays until Enter |
| TC-16 | Withdraw | Decline confirmation | Quick option 1, answer `n` | `Cancelled. Nothing was changed.`; balance unchanged; no history entry |
| TC-17 | Withdraw | Minimum boundary | Option 5 (Other), enter `100` | Accepted; balance reduced by ₹100.00 |
| TC-18 | Withdraw | Per-transaction limit boundary | Deposit 100000, then withdraw `100000` and `100100` | `100000` succeeds; `100100` rejected with `The maximum per transaction is ₹1,00,000.00.` |
| TC-19 | Withdraw | Not a multiple of 100 | Other amount `250`, `150.50` | `Please enter an amount in multiples of ₹100.` and re-prompt |
| TC-20 | Withdraw | Insufficient funds | Log in as `1003` (10,000), Other `20000` | Red `✖ Insufficient Funds` with requested/available amounts; no confirmation shown; balance unchanged |
| TC-21 | Withdraw | Exact balance | As `1003`, withdraw `10000` | Succeeds; new balance ₹0.00 |
| TC-22 | Withdraw | Invalid amounts | Other amount: `0.00`, `-100`, `abc`, `100.555`, `99999999999`, empty | Each gets a specific polite message (greater than zero / negative / numbers only / 2 decimal places / too large / enter an amount); never a stack trace |
| TC-23 | Deposit | Valid deposit | Choose 3, enter `15000.50` | Receipt, new balance ₹40,000.50 |
| TC-24 | Deposit | Limit boundary | Deposit `100000` then `100000.01` | First accepted; second rejected with the maximum-per-transaction message |
| TC-25 | Deposit | Invalid amounts | `0.00`, `-5`, `abc`, `1000.555` | Specific inline message each time; balance unchanged |
| TC-26 | Deposit | Formatted input accepted | Enter `1,500` and `Rs. 250` | Both accepted as 1500.00 and 250.00 |
| TC-27 | Transfer | Success visible on both accounts | Run with `--relogin`. As `1001` transfer `1000` to `1002`, confirm, Quit, log in as `1002/4321`, open history | Sender balance ₹24,000.00 with a Transfer Out row; recipient balance ₹49,500.00 with a Transfer In row `Transfer from 1001 (Aarav Kumar)` |
| TC-28 | Transfer | Transfer to own account | Recipient `1001` while logged in as `1001` | `You cannot transfer money to your own account.`; re-prompt |
| TC-29 | Transfer | Unknown recipient | Recipient `9999` | `No account found with ID 9999...`; re-prompt |
| TC-30 | Transfer | Insufficient funds | As `1003`, transfer `50000` to `1001` | Red `Insufficient Funds`; both balances unchanged |
| TC-31 | Transfer | Decline confirmation | Valid transfer, answer `n` | Cancelled; no balance change on either account |
| TC-32 | Transfer | Confirmation summary | Valid transfer up to the Y/N prompt | Panel shows recipient name, masked recipient account, amount and balance after |
| TC-33 | Transfer | Invalid amounts and limit | `0.00`, `-5`, `abc`, `100000.01` | Same validation as deposit; limit message shown |
| TC-34 | Cancel | Cancel at quick-select | Withdraw screen, type `0` | Returns to menu after `Cancelled`; nothing changed |
| TC-35 | Cancel | Cancel at amount prompt | Deposit / Other amount, type `b` (or `B`) | Cancelled; back to menu |
| TC-36 | Cancel | Cancel at recipient prompt | Transfer, recipient `b` | Cancelled; back to menu |
| TC-37 | Input | Whitespace tolerance | Type ` 1 ` at the menu | Treated as `1` |
| TC-38 | Input | EOF at login | Press Ctrl+D (Ctrl+Z then Enter on Windows) at User ID, or run `java ... < /dev/null` | `Input closed. Ending the session safely. Goodbye!`; clean exit, no stack trace |
| TC-39 | Input | EOF mid-transaction | Press Ctrl+D at the withdraw amount prompt | Same graceful message; balance unchanged |
| TC-40 | Input | Y/N validation | At `Confirm?` type `maybe`, empty, `Y` | Error `Please type Y for yes or N for no.` for the first two; `Y` (or `yes`) proceeds |
| TC-41 | Quit | Goodbye with summary | Make 2 transactions, choose 5 | Goodbye panel: transaction count, net change in balance, closing balance, `Please remember to take your card.`; program ends |
| TC-42 | Quit | Quit with no activity | Login, choose 5 | Summary shows 0 transactions and net change +₹0.00 |
| TC-43 | Modes | --no-color | Run with `--no-color` | No colour and no escape codes (verify with `| cat -v`); symbols and boxes still Unicode; screen is not cleared |
| TC-44 | Modes | --ascii | Run with `--ascii` | Borders `+ - | =`, `Rs.` currency, `[OK] [X] [!] >` markers, `[H] [W] [D] [T] [Q]` menu markers; output is pure ASCII |
| TC-45 | Modes | --fast and default animation | Run with and without `--fast` in a real terminal | Without: banner then progress bar `Initializing secure session...` finishing in under 1.5 s; with `--fast`: bar skipped |
| TC-46 | Modes | Screen clearing and receipt persistence | In a real terminal complete a deposit | Screen clears when moving between screens, but the receipt stays visible until Enter is pressed |
| TC-47 | Modes | Layout width | Resize terminal to 80 columns, open history | No line wraps; table fully aligned |
| TC-48 | Modes | Flags | Run with `--help`, then `--bogus` | `--help` prints usage (exit 0); `--bogus` prints usage and exits with code 2 |
| TC-49 | Modes | Piped / non-TTY output | `echo ... | java -cp out com.atm.Main` | No ANSI codes, no animation or clearing, static `Initializing secure session... done` line |

## Automated tests

Unit and end-to-end tests live in `src/test/java` (JUnit 5): `AccountTest`, `BankTest`, `LoginAttemptTrackerTest`, `AtmSessionTest` (scripted keyboard sessions, including lock-out, EOF, ASCII mode and the 80-column check), `CurrencyFormatterTest`, `InputHelperTest`, `ConsoleUITest`. Run with `mvn test`.
