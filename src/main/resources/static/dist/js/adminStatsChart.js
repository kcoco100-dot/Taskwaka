const ctx = document.getElementById('monthlyRevenueChart');
const months = ctx.dataset.months.split(',');
const amounts = ctx.dataset.amounts.split(',').map(Number);

new Chart(ctx, {
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