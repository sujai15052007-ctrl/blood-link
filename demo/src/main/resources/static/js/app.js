const API = "/api";
const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

async function apiRequest(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  if (response.status === 204) return null;
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const details = payload.fieldErrors && Object.values(payload.fieldErrors).length
      ? Object.values(payload.fieldErrors).join(" ")
      : payload.message;
    throw new Error(details || `Request failed (${response.status}).`);
  }
  return payload;
}

function showNotice(element, message, kind = "success") {
  element.textContent = message;
  element.className = `notice notice-${kind}`;
  element.hidden = false;
}

function hideNotice(element) {
  element.hidden = true;
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}

function safeTelephone(value) {
  return String(value ?? "").replace(/[^\d+().\- ]/g, "");
}

function formatDate(value) {
  if (!value) return "Never";
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: "numeric", month: "short", year: "numeric"
  });
}

function bindNavigation() {
  const toggle = document.querySelector(".menu-toggle");
  const nav = document.querySelector(".main-nav");
  toggle?.addEventListener("click", () => {
    const expanded = toggle.getAttribute("aria-expanded") === "true";
    toggle.setAttribute("aria-expanded", String(!expanded));
    nav.classList.toggle("nav-open", !expanded);
  });
}

async function loadDashboard() {
  const error = document.querySelector("#dashboard-error");
  try {
    const [stats, groups] = await Promise.all([
      apiRequest(`${API}/dashboard/stats`),
      apiRequest(`${API}/dashboard/blood-groups`)
    ]);
    document.querySelector("#total-donors").textContent = stats.totalDonors;
    document.querySelector("#available-donors").textContent = stats.availableDonors;
    document.querySelector("#unavailable-donors").textContent = stats.unavailableDonors;
    document.querySelector("#blood-group-grid").innerHTML = groups.map(group => `
      <a class="blood-group-item" href="search.html?bloodGroup=${encodeURIComponent(group.label)}">
        <span class="blood-type">${escapeHtml(group.label)}</span>
        <span class="blood-count"><b>${group.total}</b><small>${group.available} available</small></span>
        <span class="mini-bar"><i style="width:${group.total ? Math.max(8, Math.round(group.available / group.total * 100)) : 0}%"></i></span>
      </a>`).join("");
    hideNotice(error);
  } catch (exception) {
    showNotice(error, `Could not load dashboard data: ${exception.message}`, "error");
    document.querySelector("#blood-group-grid").innerHTML = '<p class="empty-state">Dashboard data is unavailable. Check the server connection.</p>';
  }
}

function donorCard(donor) {
  const initials = donor.name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase();
  return `<article class="donor-card">
    <div class="donor-card-top"><span class="avatar">${escapeHtml(initials)}</span><span class="blood-type">${escapeHtml(donor.bloodGroupLabel)}</span></div>
    <h3>${escapeHtml(donor.name)}</h3><p class="donor-location"><span>⌖</span> ${escapeHtml(donor.city)}</p>
    <div class="donor-contact"><a href="tel:${safeTelephone(donor.phone)}"><span>☎</span>${escapeHtml(donor.phone)}</a><a href="mailto:${encodeURIComponent(donor.email)}"><span>✉</span>${escapeHtml(donor.email)}</a></div>
    <div class="donor-card-foot"><span class="availability"><i class="status-dot"></i> Available now</span><span>Last donation: ${formatDate(donor.lastDonationDate)}</span></div>
  </article>`;
}

async function loadSearch(params = new URLSearchParams(window.location.search)) {
  const results = document.querySelector("#search-results");
  const error = document.querySelector("#search-error");
  const group = params.get("bloodGroup") || "";
  const city = params.get("city") || "";
  const form = document.querySelector("#search-form");
  form.elements.bloodGroup.value = group;
  form.elements.city.value = city;
  const query = new URLSearchParams();
  if (group) query.set("bloodGroup", group);
  if (city) query.set("city", city);
  results.innerHTML = '<p class="loading">Searching available donors…</p>';
  try {
    const donors = await apiRequest(`${API}/donors/search${query.size ? `?${query}` : ""}`);
    document.querySelector("#result-count").textContent = donors.length;
    results.innerHTML = donors.length
      ? donors.map(donorCard).join("")
      : '<div class="empty-state empty-card"><span class="empty-icon">⌕</span><h3>No available donors found</h3><p>Try another blood group or city, or register a donor to grow the network.</p><a class="button button-primary" href="register.html">Register a donor</a></div>';
    hideNotice(error);
  } catch (exception) {
    document.querySelector("#result-count").textContent = "0";
    results.innerHTML = "";
    showNotice(error, `Could not search donors: ${exception.message}`, "error");
  }
}

async function loadDonors() {
  const body = document.querySelector("#donor-table-body");
  const error = document.querySelector("#donors-error");
  try {
    const donors = await apiRequest(`${API}/donors`);
    document.querySelector("#donor-list-count").textContent = `${donors.length} ${donors.length === 1 ? "donor" : "donors"} registered`;
    body.innerHTML = donors.length ? donors.map(donor => `
      <tr>
        <td><div class="table-person"><span class="avatar avatar-small">${escapeHtml(donor.name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join("").toUpperCase())}</span><span><b>${escapeHtml(donor.name)}</b><small>${escapeHtml(donor.email)}</small></span></div></td>
        <td><span class="blood-type table-blood">${escapeHtml(donor.bloodGroupLabel)}</span></td>
        <td>${escapeHtml(donor.city)}<small>${escapeHtml(donor.phone)}</small></td>
        <td>${formatDate(donor.lastDonationDate)}</td>
        <td><span class="status-pill ${donor.available ? "is-available" : "is-resting"}"><i></i>${donor.available ? "Available" : `Available ${formatDate(donor.nextAvailableDate)}`}</span></td>
        <td><div class="row-actions"><a class="row-action" href="register.html?id=${donor.id}" aria-label="Edit ${escapeHtml(donor.name)}">Edit</a><button class="row-action row-delete" type="button" data-delete-id="${donor.id}" data-donor-name="${escapeHtml(donor.name)}">Delete</button></div></td>
      </tr>`).join("") : '<tr><td colspan="6" class="empty-table">No donors registered yet. <a href="register.html">Add the first donor →</a></td></tr>';
    body.querySelectorAll("[data-delete-id]").forEach(button => button.addEventListener("click", async () => {
      if (!window.confirm(`Delete ${button.dataset.donorName} from the registry? Their donation records will also be removed.`)) return;
      try {
        await apiRequest(`${API}/donors/${button.dataset.deleteId}`, { method: "DELETE" });
        await loadDonors();
      } catch (exception) {
        showNotice(error, `Could not delete donor: ${exception.message}`, "error");
      }
    }));
    hideNotice(error);
  } catch (exception) {
    body.innerHTML = "";
    showNotice(error, `Could not load donors: ${exception.message}`, "error");
  }
}

async function loadDonorForEdit(form, donorId) {
  const donor = await apiRequest(`${API}/donors/${donorId}`);
  for (const field of form.elements) {
    if (field.name && donor[field.name] != null) field.value = donor[field.name];
  }
  document.querySelector("#register-title").textContent = "Update donor details.";
  document.querySelector("#register-button-label").textContent = "Save changes";
}

async function setupDonorForm() {
  const form = document.querySelector("#donor-form");
  const message = document.querySelector("#register-message");
  const donorId = new URLSearchParams(window.location.search).get("id");
  if (donorId) {
    try {
      await loadDonorForEdit(form, donorId);
    } catch (exception) {
      showNotice(message, `Could not load donor: ${exception.message}`, "error");
      return;
    }
  }
  form.addEventListener("submit", async event => {
    event.preventDefault();
    hideNotice(message);
    const data = Object.fromEntries(new FormData(form));
    data.lastDonationDate = data.lastDonationDate || null;
    try {
      await apiRequest(donorId ? `${API}/donors/${donorId}` : `${API}/donors`, {
        method: donorId ? "PUT" : "POST",
        body: JSON.stringify(data)
      });
      showNotice(message, donorId ? "Donor details updated successfully." : "Donor registered successfully.", "success");
      if (!donorId) form.reset();
      window.setTimeout(() => { window.location.href = "donors.html"; }, 900);
    } catch (exception) {
      showNotice(message, exception.message, "error");
    }
  });
}

async function setupDonationForm() {
  const form = document.querySelector("#donation-form");
  const message = document.querySelector("#donation-message");
  const donorSelect = form.elements.donorId;
  const today = new Date().toISOString().slice(0, 10);
  form.elements.donationDate.max = today;
  form.elements.donationDate.value = today;
  try {
    const donors = await apiRequest(`${API}/donors`);
    donorSelect.innerHTML = '<option value="">Choose a donor</option>' + donors.map(donor => `
      <option value="${donor.id}">${escapeHtml(donor.name)} · ${escapeHtml(donor.bloodGroupLabel)} · ${escapeHtml(donor.city)}${donor.available ? "" : " · in recovery"}</option>`).join("");
    const selectedId = new URLSearchParams(window.location.search).get("donorId");
    if (selectedId) donorSelect.value = selectedId;
  } catch (exception) {
    donorSelect.innerHTML = '<option value="">Could not load donors</option>';
    showNotice(message, `Could not load donor list: ${exception.message}`, "error");
    return;
  }
  form.addEventListener("submit", async event => {
    event.preventDefault();
    hideNotice(message);
    const request = Object.fromEntries(new FormData(form));
    request.donorId = Number(request.donorId);
    try {
      const donation = await apiRequest(`${API}/donations`, { method: "POST", body: JSON.stringify(request) });
      showNotice(message, `Donation recorded for ${donation.donorName}. The donor is now in a 90-day recovery period.`, "success");
      setupDonationOptionsOnly(donorSelect).catch(exception => {
        showNotice(message, `Donation recorded, but the donor list could not be refreshed: ${exception.message}`, "success");
      });
    } catch (exception) {
      showNotice(message, exception.message, "error");
    }
  });
}

async function setupDonationOptionsOnly(select) {
  const selected = select.value;
  const donors = await apiRequest(`${API}/donors`);
  select.innerHTML = '<option value="">Choose a donor</option>' + donors.map(donor => `
    <option value="${donor.id}">${escapeHtml(donor.name)} · ${escapeHtml(donor.bloodGroupLabel)} · ${escapeHtml(donor.city)}${donor.available ? "" : " · in recovery"}</option>`).join("");
  select.value = selected;
}

document.addEventListener("DOMContentLoaded", () => {
  bindNavigation();
  const page = document.body.dataset.page;
  if (page === "dashboard") loadDashboard();
  if (page === "search") {
    document.querySelector("#search-form").addEventListener("submit", event => {
      event.preventDefault();
      const values = new FormData(event.currentTarget);
      const params = new URLSearchParams();
      if (values.get("bloodGroup")) params.set("bloodGroup", values.get("bloodGroup"));
      if (values.get("city")) params.set("city", values.get("city").trim());
      history.replaceState(null, "", `${window.location.pathname}${params.size ? `?${params}` : ""}`);
      loadSearch(params);
    });
    loadSearch();
  }
  if (page === "donors") loadDonors();
  if (page === "register") setupDonorForm();
  if (page === "donation") setupDonationForm();
});
