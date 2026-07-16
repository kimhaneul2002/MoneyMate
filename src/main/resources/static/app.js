const API = "";

const state = {
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  items: [],
  categories: [],
  selectedDate: null,
  editingId: null,
};

const el = (id) => document.getElementById(id);

// ---------- 유틸 ----------
function money(n) {
  return Number(n).toLocaleString("ko-KR") + "원";
}

function todayStr() {
  const d = new Date();
  return d.toISOString().slice(0, 10);
}

function showToast(msg) {
  const t = el("toast");
  t.textContent = msg;
  t.classList.add("show");
  setTimeout(() => t.classList.remove("show"), 2000);
}

function openOverlay(id) { el(id).classList.add("open"); }
function closeOverlay(id) { el(id).classList.remove("open"); }

// ---------- 데이터 로딩 ----------
async function loadCategories() {
  const res = await fetch(`${API}/api/categories`);
  state.categories = await res.json();
  const sel = el("fCategory");
  sel.innerHTML = "";

  const income = state.categories.filter(c => c.type === "INCOME");
  const expense = state.categories.filter(c => c.type === "EXPENSE");

  if (income.length) {
    const g = document.createElement("optgroup");
    g.label = "수입";
    income.forEach(c => g.appendChild(new Option(c.name, c.id)));
    sel.appendChild(g);
  }
  if (expense.length) {
    const g = document.createElement("optgroup");
    g.label = "지출";
    expense.forEach(c => g.appendChild(new Option(c.name, c.id)));
    sel.appendChild(g);
  }
}

async function loadMonth() {
  const res = await fetch(`${API}/api/ledger?year=${state.year}&month=${state.month}`);
  const data = await res.json();
  state.items = data.items || [];
  el("totalIncome").textContent = money(data.totalIncome || 0);
  el("totalExpense").textContent = money(data.totalExpense || 0);
  renderCalendar();
}

// ---------- 달력 렌더 ----------
function renderCalendar() {
  el("monthLabel").textContent = `${state.year}년 ${state.month}월`;

  const grid = el("calendarGrid");
  grid.innerHTML = "";

  const firstDay = new Date(state.year, state.month - 1, 1);
  const startOffset = firstDay.getDay();
  const daysInMonth = new Date(state.year, state.month, 0).getDate();
  const today = todayStr();

  const byDate = {};
  state.items.forEach(item => {
    (byDate[item.transactionDate] ||= []).push(item);
  });

  for (let i = 0; i < startOffset; i++) {
    const cell = document.createElement("div");
    cell.className = "day-cell empty";
    grid.appendChild(cell);
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${state.year}-${String(state.month).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    const cell = document.createElement("div");
    cell.className = "day-cell" + (dateStr === today ? " today" : "");

    const num = document.createElement("div");
    num.className = "day-num";
    num.textContent = d;
    cell.appendChild(num);

    const dayItems = byDate[dateStr] || [];
    const shown = dayItems.slice(0, 2);
    shown.forEach(item => {
      const isIncome = item.category && item.category.type === "INCOME";
      const badge = document.createElement("div");
      badge.className = "day-badge " + (isIncome ? "income" : "expense");
      badge.textContent = (isIncome ? "+" : "-") + Number(item.amount).toLocaleString("ko-KR");
      cell.appendChild(badge);
    });
    if (dayItems.length > 2) {
      const more = document.createElement("div");
      more.className = "day-badge more";
      more.textContent = `+${dayItems.length - 2}건`;
      cell.appendChild(more);
    }

    cell.addEventListener("click", () => openDay(dateStr));
    grid.appendChild(cell);
  }
}

// ---------- 날짜 상세 ----------
function openDay(dateStr) {
  state.selectedDate = dateStr;
  const [y, m, d] = dateStr.split("-");
  el("dayTitle").textContent = `${Number(m)}월 ${Number(d)}일`;

  const list = el("dayList");
  list.innerHTML = "";

  const dayItems = state.items.filter(item => item.transactionDate === dateStr);
  if (dayItems.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-msg";
    empty.textContent = "이 날짜에 등록된 내역이 없습니다.";
    list.appendChild(empty);
  } else {
    dayItems.forEach(item => {
      const isIncome = item.category && item.category.type === "INCOME";
      const row = document.createElement("div");
      row.className = "item-row";
      row.innerHTML = `
        <div class="item-main">
          <div class="item-title"></div>
          <div class="item-cat"></div>
        </div>
        <div class="item-amount ${isIncome ? "income" : "expense"}"></div>
      `;
      row.querySelector(".item-title").textContent = item.title;
      row.querySelector(".item-cat").textContent = item.category ? item.category.name : "";
      row.querySelector(".item-amount").textContent = (isIncome ? "+" : "-") + money(item.amount);
      row.addEventListener("click", () => openForm("edit", item));
      list.appendChild(row);
    });
  }

  openOverlay("dayOverlay");
}

// ---------- 등록/수정 폼 ----------
function openForm(mode, item) {
  el("formError").textContent = "";
  el("ledgerForm").reset();

  if (mode === "edit") {
    state.editingId = item.id;
    el("formTitle").textContent = "내역 수정";
    el("fTransactionDate").value = item.transactionDate;
    el("fTitle").value = item.title;
    el("fAmount").value = item.amount;
    el("fCategory").value = item.category ? item.category.id : "";
    el("deleteBtn").style.display = "inline-flex";
  } else {
    state.editingId = null;
    el("formTitle").textContent = "내역 추가";
    el("fTransactionDate").value = state.selectedDate || todayStr();
    el("deleteBtn").style.display = "none";
  }

  closeOverlay("dayOverlay");
  openOverlay("formOverlay");
}

async function submitForm(e) {
  e.preventDefault();
  el("formError").textContent = "";

  const payload = {
    title: el("fTitle").value,
    amount: Number(el("fAmount").value),
    transactionDate: el("fTransactionDate").value,
    category: { id: Number(el("fCategory").value) },
  };

  const isEdit = state.editingId !== null;
  const url = isEdit ? `${API}/api/ledger/${state.editingId}` : `${API}/api/ledger`;
  const method = isEdit ? "PUT" : "POST";

  try {
    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.json();
      const msg = Object.values(err)[0] || "요청을 처리할 수 없습니다.";
      el("formError").textContent = msg;
      return;
    }

    closeOverlay("formOverlay");
    showToast(isEdit ? "내역이 수정되었습니다." : "내역이 등록되었습니다.");
    await loadMonth();
  } catch (err) {
    el("formError").textContent = "서버와 통신할 수 없습니다.";
  }
}

async function deleteItem() {
  if (state.editingId === null) return;
  if (!confirm("이 내역을 삭제할까요?")) return;

  try {
    const res = await fetch(`${API}/api/ledger/${state.editingId}`, { method: "DELETE" });
    const data = await res.json();
    closeOverlay("formOverlay");
    showToast(data.message || "삭제되었습니다.");
    await loadMonth();
  } catch (err) {
    el("formError").textContent = "삭제에 실패했습니다.";
  }
}

// ---------- AI 분석 ----------
async function openAiAnalysis() {
  openOverlay("aiOverlay");
  el("aiLoading").style.display = "flex";
  el("aiResult").style.display = "none";

  try {
    const res = await fetch(`${API}/api/ledger/ai-analysis?year=${state.year}&month=${state.month}`);
    const text = await res.text();
    el("aiResult").textContent = text;
  } catch (err) {
    el("aiResult").textContent = "분석 결과를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    el("aiLoading").style.display = "none";
    el("aiResult").style.display = "block";
  }
}

// ---------- 이벤트 바인딩 ----------
el("prevMonth").addEventListener("click", () => {
  state.month--;
  if (state.month < 1) { state.month = 12; state.year--; }
  loadMonth();
});

el("nextMonth").addEventListener("click", () => {
  state.month++;
  if (state.month > 12) { state.month = 1; state.year++; }
  loadMonth();
});

el("addBtn").addEventListener("click", () => {
  state.selectedDate = todayStr();
  openForm("create");
});

el("addOnDayBtn").addEventListener("click", () => openForm("create"));

el("closeDayBtn").addEventListener("click", () => closeOverlay("dayOverlay"));
el("closeFormBtn").addEventListener("click", () => closeOverlay("formOverlay"));
el("closeAiBtn").addEventListener("click", () => closeOverlay("aiOverlay"));

el("aiBtn").addEventListener("click", openAiAnalysis);
el("ledgerForm").addEventListener("submit", submitForm);
el("deleteBtn").addEventListener("click", deleteItem);

[["dayOverlay"], ["formOverlay"], ["aiOverlay"]].forEach(([id]) => {
  el(id).addEventListener("click", (e) => {
    if (e.target === e.currentTarget) closeOverlay(id);
  });
});

// ---------- 초기 로딩 ----------
(async function init() {
  await loadCategories();
  await loadMonth();
})();
