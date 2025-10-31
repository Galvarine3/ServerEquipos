// Basic client script
const app = document.getElementById('app');

function render() {
  const now = new Date();
  app.innerHTML = `
    <div class="card">
      <h3>Hola 👋</h3>
      <p>La web está corriendo correctamente.</p>
      <p><strong>Hora:</strong> ${now.toLocaleString()}</p>
      <button id="ping">Probar /health</button>
      <pre id="out" class="pre"></pre>
    </div>
  `;

  document.getElementById('ping').addEventListener('click', async () => {
    const res = await fetch('/health');
    const data = await res.json();
    document.getElementById('out').textContent = JSON.stringify(data, null, 2);
  });
}

render();

// Update year in footer
const year = document.getElementById('year');
if (year) year.textContent = new Date().getFullYear();
