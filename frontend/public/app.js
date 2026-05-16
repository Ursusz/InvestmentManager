const API = {
  deposit: "/api/deposit",
  withdraw: "/api/withdraw",
  statement: "/api/generateAccountSatement",
  reset: "/api/resetAccount",
  backfill: "/api/backFillMonthlyInterest"
};

const state = {
  mode: "deposit",
  transactions: []
};

const el = {
  depositTab: document.querySelector("#depositTab"),
  withdrawTab: document.querySelector("#withdrawTab"),
  form: document.querySelector("#transactionForm"),
  amount: document.querySelector("#amount"),
  description: document.querySelector("#description"),
  dateField: document.querySelector("#dateField"),
  transactionDate: document.querySelector("#transactionDate"),
  submitButton: document.querySelector("#submitButton"),
  message: document.querySelector("#message"),
  backfillMonthsButton: document.querySelector("#backfillMonths"),
  statementBody: document.querySelector("#statementBody"),
  transactionCount: document.querySelector("#transactionCount"),
  depositTotal: document.querySelector("#depositTotal"),
  withdrawalTotal: document.querySelector("#withdrawalTotal"),
  interestTotal: document.querySelector("#interestTotal"),
  taxTotal: document.querySelector("#taxTotal"),
  accountBalance: document.querySelector("#balance"),
  resetAccount: document.querySelector("#resetAccount")
};

function setDefaultDate() {
  el.transactionDate.value = new Date().toISOString().slice(0, 10);
}

function setMode(mode) {
  state.mode = mode;
  const isDeposit = mode === "deposit";

  el.depositTab.classList.toggle("active", isDeposit);
  el.withdrawTab.classList.toggle("active", !isDeposit);
  el.dateField.hidden = !isDeposit;
  el.transactionDate.required = isDeposit;
  el.submitButton.textContent = isDeposit ? "Add Deposit" : "Add Withdrawal";
  setMessage("");
}

function setMessage(text, type = "") {
  el.message.textContent = text;
  el.message.className = type ? `message ${type}` : "message";
}

function money(value) {
  return Number(value || 0).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function signedAmount(transaction) {
  const amount = Number(transaction.amount || 0);
  if (transaction.type === "Withdrawal" || transaction.type === "Tax") {
    return -Math.abs(amount);
  }
  return amount;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function badgeClass(type) {
  return `badge badge-${String(type || "unknown").toLowerCase()}`;
}

function renderSummary() {
  const totals = state.transactions.reduce((acc, tx) => {
    const amount = Number(tx.amount || 0);

    if (tx.type === "Deposit") {
      acc.deposits += amount;
      acc.balance += amount;
    }
    if (tx.type === "Withdrawal"){
      acc.withdrawals += amount;
      acc.balance -= amount;
    }
    if (tx.type === "Interest"){
      acc.interest += amount;
      acc.balance += amount;
    }
    if (tx.type === "Tax"){
      acc.tax += amount;
      acc.balance -= amount;
    }

    return acc;
  }, { deposits: 0, withdrawals: 0, interest: 0, tax: 0, balance: 0 });

  el.depositTotal.textContent = money(totals.deposits);
  el.withdrawalTotal.textContent = money(totals.withdrawals);
  el.interestTotal.textContent = money(totals.interest);
  el.taxTotal.textContent = money(totals.tax);
  el.accountBalance.textContent = money(totals.balance);
}

function renderStatement() {
  el.transactionCount.textContent = `${state.transactions.length} transaction${state.transactions.length === 1 ? "" : "s"}`;

  if (state.transactions.length === 0) {
    el.statementBody.innerHTML = `<tr><td class="empty" colspan="5">No transactions found.</td></tr>`;
    renderSummary();
    return;
  }

  el.statementBody.innerHTML = state.transactions.map((tx) => `
    <tr>
      <td>${escapeHtml(tx.id)}</td>
      <td><span class="${badgeClass(tx.type)}">${escapeHtml(tx.type)}</span></td>
      <td class="amount">${money(signedAmount(tx))}</td>
      <td>${escapeHtml(tx.details || "")}</td>
      <td>${escapeHtml(tx.date || "")}</td>
    </tr>
  `).join("");

  renderSummary();
}

async function refreshStatement() {
  try {
    const response = await fetch(API.statement);
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }

    const parsed = text.trim().startsWith("[") ? JSON.parse(text) : [];
    state.transactions = Array.isArray(parsed) ? parsed : [];
    renderStatement();
  } catch (error) {
    state.transactions = [];
    renderSummary();
    el.transactionCount.textContent = "0 transactions";
    el.statementBody.innerHTML = `<tr><td class="empty" colspan="5">${escapeHtml(error.message)}</td></tr>`;
  }
}

async function triggerBackfill() {
  el.backfillMonthsButton.disabled = true;

  try {
    const response = await fetch(API.backfill, {method: "POST"});
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }

    await refreshStatement();
  } catch (error) {
    setMessage(error.message, "error");
  } finally {
    el.backfillMonthsButton.disabled = false;
  }
}

function depositParams() {
  const date = new Date(`${el.transactionDate.value}T00:00:00`);

  return new URLSearchParams({
    amount: el.amount.value,
    description: el.description.value.trim(),
    day: String(date.getDate()),
    month: String(date.getMonth() + 1),
    year: String(date.getFullYear())
  });
}

function withdrawParams() {
  return new URLSearchParams({
    amount: el.amount.value,
    description: el.description.value.trim()
  });
}

async function submitTransaction(event) {
  event.preventDefault();
  setMessage("");

  const isDeposit = state.mode === "deposit";
  const endpoint = isDeposit ? API.deposit : API.withdraw;
  const params = isDeposit ? depositParams() : withdrawParams();

  el.submitButton.disabled = true;

  try {
    const response = await fetch(`${endpoint}?${params.toString()}`, { method: "POST" });
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }

    await refreshStatement();
    el.form.reset();
    setDefaultDate();
    setMessage(text || (isDeposit ? "Deposit request processed." : "Withdrawal request processed."), responseLooksRejected(text) ? "error" : "success");
  } catch (error) {
    setMessage(error.message, "error");
  } finally {
    el.submitButton.disabled = false;
  }
}

function responseLooksRejected(text) {
  return /cannot|insufficient|processed|unknown|error/i.test(text || "");
}

async function resetAccount(){
  el.resetAccount.disabled = true;

  try {
    const response = await fetch(API.reset);
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }

    await refreshStatement();
    setMessage(text || "Account reset.", "success");
  } catch (error) {
    setMessage(error.message, "error");
  } finally {
    el.resetAccount.disabled = false;
  }
}

el.depositTab.addEventListener("click", () => setMode("deposit"));
el.withdrawTab.addEventListener("click", () => setMode("withdraw"));
el.form.addEventListener("submit", submitTransaction);
el.resetAccount.addEventListener("click", resetAccount);
el.backfillMonthsButton.addEventListener("click", triggerBackfill);


setDefaultDate();
setMode("deposit");
refreshStatement();
