
const startDate = document.getElementById("startDate").value;
const endDate = document.getElementById("endDate").value;

flatpickr("#dateRange", {
    mode: "range",
    dateFormat: "Y-m-d",
	defaultDate: startDate && endDate ? [startDate, endDate] : [],
    onChange: function(selectedDates, dateStr, instance) {
        if (selectedDates.length === 2) {
            document.getElementById("startDate").value = instance.formatDate(selectedDates[0], "Y-m-d");
            document.getElementById("endDate").value = instance.formatDate(selectedDates[1], "Y-m-d");
        } else {
            document.getElementById("startDate").value = "";
            document.getElementById("endDate").value = "";
        }
    }
});

// 수익 통계 차트.js
const ctx = document.getElementById('monthlyRevenueChart');
const months = ctx.dataset.months.split(',');
const amounts = ctx.dataset.amounts.split(',').map(Number);
const payments = ctx.dataset.payments.split(',').map(Number);
const refunds = ctx.dataset.refunds.split(',').map(Number);

const revenueChart = new Chart(ctx, {
    type: 'bar',
    data: {
        labels: months,
        datasets: [{
            label: '순결제 금액',
            data: amounts
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true
            }
        }
    }
});

document.getElementById("revenueType").addEventListener("change", function() {

    const type = this.value;

    if (type === "payment") {
        revenueChart.data.datasets[0].label = "결제 금액";
        revenueChart.data.datasets[0].data = payments;
    } else if (type === "refund") {
        revenueChart.data.datasets[0].label = "환불 금액";
        revenueChart.data.datasets[0].data = refunds;
    } else {
        revenueChart.data.datasets[0].label = "순결제 금액";
        revenueChart.data.datasets[0].data = amounts;
    }

    revenueChart.update();
});