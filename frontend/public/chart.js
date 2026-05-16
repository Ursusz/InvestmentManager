const API = {
  monthlyReport: "/api/generateMonthlyReport"
};

const el = {
  backButton: document.querySelector("#backButton"),
  refreshChartButton: document.querySelector("#refreshChartButton"),
  chartStatus: document.querySelector("#chartStatus"),
  canvas: document.querySelector("#balanceChart"),
  totalInvestment: document.querySelector("#totalInvestment"),
  currentBalance: document.querySelector("#currentBalance"),
  netProfit: document.querySelector("#netProfit"),
  profitPercentage: document.querySelector("#profitPercentage")
};

const ctx = el.canvas.getContext("2d");

function money(value) {
  return Number(value || 0).toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function parseReport(text) {
  if (!text || !text.trim().startsWith("{")) {
    return { points: [] };
  }

  const report = JSON.parse(text);
  return {
    ...report,
    points: Array.isArray(report.points) ? report.points : []
  };
}

function normalizePoints(points) {
  return points
    .map((point) => ({
      month: point.month || formatMonth(point.date),
      balance: Number(point.accountBalance ?? point.balance ?? 0)
    }))
    .filter((point) => point.month && Number.isFinite(point.balance));
}

function formatMonth(dateValue) {
  if (!dateValue) {
    return "";
  }

  const date = new Date(`${dateValue}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return String(dateValue);
  }

  return date.toLocaleDateString("en-US", {
    month: "short",
    year: "numeric"
  });
}

function setSummary(report) {
  el.totalInvestment.textContent = money(report.totalInvestment);
  el.currentBalance.textContent = money(report.currentBalance);
  el.netProfit.textContent = money(report.netProfit);
  el.profitPercentage.textContent = `${money(report.profitPercentage)}%`;
}

function drawEmpty(message) {
  const width = el.canvas.width;
  const height = el.canvas.height;

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#627181";
  ctx.font = "16px Segoe UI, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(message, width / 2, height / 2);
}

function drawChart(points) {
  if (points.length === 0) {
    drawEmpty("No monthly balance data available.");
    return;
  }

  const width = el.canvas.width;
  const height = el.canvas.height;
  const padding = { top: 34, right: 34, bottom: 82, left: 86 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;
  const balances = points.map((point) => point.balance);
  const minValue = Math.min(0, ...balances);
  const maxValue = Math.max(...balances);
  const valueRange = maxValue - minValue || 1;

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, width, height);

  ctx.strokeStyle = "#d8e1e8";
  ctx.lineWidth = 1;
  ctx.fillStyle = "#627181";
  ctx.font = "13px Segoe UI, sans-serif";
  ctx.textAlign = "right";
  ctx.textBaseline = "middle";

  for (let i = 0; i <= 5; i++) {
    const ratio = i / 5;
    const y = padding.top + chartHeight - ratio * chartHeight;
    const value = minValue + ratio * valueRange;

    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
    ctx.fillText(money(value), padding.left - 12, y);
  }

  const xFor = (index) => {
    if (points.length === 1) {
      return padding.left + chartWidth / 2;
    }
    return padding.left + (index / (points.length - 1)) * chartWidth;
  };
  const yFor = (balance) => padding.top + chartHeight - ((balance - minValue) / valueRange) * chartHeight;

  ctx.strokeStyle = "#116a7b";
  ctx.lineWidth = 3;
  ctx.beginPath();
  points.forEach((point, index) => {
    const x = xFor(index);
    const y = yFor(point.balance);

    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });
  ctx.stroke();

  ctx.fillStyle = "#116a7b";
  points.forEach((point, index) => {
    const x = xFor(index);
    const y = yFor(point.balance);

    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();
  });

  ctx.fillStyle = "#17202a";
  ctx.font = "13px Segoe UI, sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "top";
  points.forEach((point, index) => {
    const x = xFor(index);
    const y = height - padding.bottom + 18;

    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(-Math.PI / 5);
    ctx.fillText(point.month, 0, 0);
    ctx.restore();
  });

  ctx.fillStyle = "#627181";
  ctx.font = "14px Segoe UI, sans-serif";
  ctx.textAlign = "center";
  ctx.fillText("Month - Year", padding.left + chartWidth / 2, height - 24);

  ctx.save();
  ctx.translate(24, padding.top + chartHeight / 2);
  ctx.rotate(-Math.PI / 2);
  ctx.fillText("Account Balance", 0, 0);
  ctx.restore();
}

async function loadChart() {
  el.refreshChartButton.disabled = true;
  el.chartStatus.textContent = "Loading";

  try {
    const response = await fetch(API.monthlyReport);
    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || `Request failed with status ${response.status}`);
    }

    const report = parseReport(text);
    const points = normalizePoints(report.points);

    setSummary(report);
    drawChart(points);
    el.chartStatus.textContent = `${points.length} month${points.length === 1 ? "" : "s"}`;
  } catch (error) {
    drawEmpty(error.message);
    el.chartStatus.textContent = "Error";
  } finally {
    el.refreshChartButton.disabled = false;
  }
}

el.backButton.addEventListener("click", () => {
  window.location.href = "index.html";
});
el.refreshChartButton.addEventListener("click", loadChart);

loadChart();
