// DHOBI TRACKER - INTERACTIVE APP ENGINE
(function() {
  'use strict';

  // DEFAULT 14 INDIAN CLOTHING CATEGORIES
  const DEFAULT_CATEGORIES = [
    { name: "Kurta", active: true, default: true },
    { name: "Pajama", active: true, default: true },
    { name: "Shirt", active: true, default: true },
    { name: "T-Shirt", active: true, default: true },
    { name: "Pant", active: true, default: true },
    { name: "Lower", active: true, default: true },
    { name: "Shorts", active: true, default: true },
    { name: "Bedsheet", active: true, default: true },
    { name: "Pillow Cover", active: true, default: true },
    { name: "Towel", active: true, default: true },
    { name: "Duppata", active: true, default: true },
    { name: "Under Garments", active: true, default: true },
    { name: "Socks", active: true, default: true },
    { name: "Hankey", active: true, default: true }
  ];

  // STATE MANAGEMENT
  let state = {
    records: [],
    categories: [],
    defaultDhobiName: "Raju Dhobi",
    currentRecordId: null,
    editingRecordId: null,
    tempChitImage: null,
    activeScreen: 'screen-home',
    filterStatus: 'ALL',
    filterDate: 'ALL',
    searchQuery: '',
    addQuantities: {},
    ocrQuantities: {}
  };

  // INITIALIZATION
  function init() {
    loadFromLocalStorage();
    setupClock();
    setupNavigation();
    setupEventListeners();

    if (state.records.length === 0) {
      loadSampleData(false);
    }

    renderCurrentScreen();
  }

  function loadFromLocalStorage() {
    try {
      const savedRecords = localStorage.getItem('dhobi_records');
      state.records = savedRecords ? JSON.parse(savedRecords) : [];

      const savedCategories = localStorage.getItem('dhobi_categories');
      state.categories = savedCategories ? JSON.parse(savedCategories) : DEFAULT_CATEGORIES;

      const savedDhobi = localStorage.getItem('dhobi_default_name');
      if (savedDhobi) state.defaultDhobiName = savedDhobi;
    } catch (e) {
      console.error("Storage error:", e);
      state.records = [];
      state.categories = DEFAULT_CATEGORIES;
    }
  }

  function saveToLocalStorage() {
    localStorage.setItem('dhobi_records', JSON.stringify(state.records));
    localStorage.setItem('dhobi_categories', JSON.stringify(state.categories));
    localStorage.setItem('dhobi_default_name', state.defaultDhobiName);
    updateDashboardStats();
  }

  function setupClock() {
    const clockEl = document.getElementById('status-clock');
    const todayEl = document.getElementById('home-today-date');
    const updateTime = () => {
      const now = new Date();
      clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
      todayEl.textContent = now.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
    };
    updateTime();
    setInterval(updateTime, 30000);
  }

  // NAVIGATION SYSTEM
  function navigateTo(screenId) {
    document.querySelectorAll('.screen-view').forEach(s => s.classList.remove('active'));
    const target = document.getElementById(screenId);
    if (target) {
      target.classList.add('active');
      state.activeScreen = screenId;
    }

    // Sync bottom nav active icon
    document.querySelectorAll('.bottom-nav-bar .nav-item').forEach(item => {
      item.classList.toggle('active', item.dataset.target === screenId);
    });

    renderCurrentScreen();
  }

  function setupNavigation() {
    // Bottom navigation tabs
    document.querySelectorAll('.bottom-nav-bar .nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        navigateTo(btn.dataset.target);
      });
    });

    // Home shortcuts
    document.getElementById('cta-add-laundry').addEventListener('click', () => startNewLaundryBag());
    document.getElementById('btn-see-all-history').addEventListener('click', () => navigateTo('screen-history'));

    // Back buttons
    document.getElementById('btn-back-add').addEventListener('click', () => navigateTo('screen-home'));
    document.getElementById('btn-cancel-add').addEventListener('click', () => navigateTo('screen-home'));
    document.getElementById('btn-back-details').addEventListener('click', () => navigateTo('screen-home'));
    document.getElementById('btn-cancel-ocr').addEventListener('click', () => navigateTo('screen-add'));
    document.getElementById('btn-discard-ocr').addEventListener('click', () => navigateTo('screen-add'));
  }

  // SCREEN RENDERING DISPATCHER
  function renderCurrentScreen() {
    switch (state.activeScreen) {
      case 'screen-home':
        renderHomeScreen();
        break;
      case 'screen-history':
        renderHistoryScreen();
        break;
      case 'screen-settings':
        renderSettingsScreen();
        break;
      case 'screen-details':
        renderDetailsScreen();
        break;
    }
  }

  // ==================== SCREEN 1: HOME ====================
  function renderHomeScreen() {
    updateDashboardStats();
    const container = document.getElementById('home-records-list');
    container.innerHTML = '';

    const recent = [...state.records].sort((a, b) => b.createdAt - a.createdAt).slice(0, 5);

    if (recent.length === 0) {
      container.innerHTML = `
        <div class="ui-card empty-state">
          <div class="empty-icon">🧺</div>
          <h4>No Laundry Bags Yet</h4>
          <p>When you give clothes to your local dhobi, tap 'Add Laundry Bag' above to record items and preserve the paper chit.</p>
        </div>
      `;
      return;
    }

    recent.forEach(record => {
      container.appendChild(createRecordCardElement(record));
    });
  }

  function updateDashboardStats() {
    let bagsAtDhobi = 0;
    let itemsAwaiting = 0;
    let partiallyReturned = 0;
    let totalCollected = 0;

    state.records.forEach(r => {
      const status = computeRecordStatus(r);
      const given = r.items.reduce((sum, i) => sum + i.quantityGiven, 0);
      const returned = r.items.reduce((sum, i) => sum + i.quantityReturned, 0);
      const pending = Math.max(0, given - returned);

      if (status === 'PENDING') {
        bagsAtDhobi++;
        itemsAwaiting += pending;
      } else if (status === 'PARTIALLY_RETURNED') {
        bagsAtDhobi++;
        partiallyReturned++;
        itemsAwaiting += pending;
      } else if (status === 'COLLECTED') {
        totalCollected++;
      }
    });

    document.getElementById('stat-bags-at-dhobi').textContent = bagsAtDhobi;
    document.getElementById('stat-items-awaiting').textContent = itemsAwaiting;
    document.getElementById('stat-partially-returned').textContent = partiallyReturned;
    document.getElementById('stat-collected-bags').textContent = totalCollected;
  }

  function computeRecordStatus(record) {
    if (record.forceCollected) return 'COLLECTED';
    const given = record.items.reduce((sum, i) => sum + i.quantityGiven, 0);
    const returned = record.items.reduce((sum, i) => sum + i.quantityReturned, 0);
    if (returned === 0) return 'PENDING';
    if (returned >= given) return 'COLLECTED';
    return 'PARTIALLY_RETURNED';
  }

  function createRecordCardElement(record) {
    const card = document.createElement('div');
    card.className = 'record-card';
    const status = computeRecordStatus(record);

    const totalGiven = record.items.reduce((sum, i) => sum + i.quantityGiven, 0);
    const totalReturned = record.items.reduce((sum, i) => sum + i.quantityReturned, 0);
    const pendingCount = totalGiven - totalReturned;

    const statusClass = status === 'PENDING' ? 'status-pending' : (status === 'COLLECTED' ? 'status-collected' : 'status-partial');
    const statusText = status === 'PENDING' ? 'Pending' : (status === 'COLLECTED' ? 'Collected' : 'Partially Back');

    card.innerHTML = `
      <div class="record-thumb-box">
        ${record.chitImage ? `<img src="${record.chitImage}" alt="Chit">` : '🧾'}
      </div>
      <div class="record-info-col">
        <div class="record-row-top">
          <span class="record-id">${record.id}</span>
          <span class="status-pill ${statusClass}">${statusText}</span>
        </div>
        <div class="record-meta-line">
          ${formatDateDisplay(record.dateGiven)} ${record.slipNumber ? `• Slip #${record.slipNumber}` : ''}
        </div>
        <div class="record-count-line">
          ${totalGiven} items ${record.dhobiName ? `• ${record.dhobiName}` : ''}
          ${pendingCount > 0 && totalReturned > 0 ? `<span style="color: var(--danger-red)"> (${pendingCount} pending)</span>` : ''}
        </div>
      </div>
      <span class="record-chevron">›</span>
    `;

    card.addEventListener('click', () => {
      openRecordDetails(record.id);
    });

    return card;
  }

  // ==================== SCREEN 2: ADD / EDIT ====================
  function startNewLaundryBag() {
    state.editingRecordId = null;
    state.tempChitImage = null;
    state.addQuantities = {};

    document.getElementById('add-screen-title').textContent = 'New Laundry Bag';
    const nextId = generateNextRecordId();
    document.getElementById('add-record-id-badge').textContent = nextId;

    document.getElementById('input-date-given').valueAsDate = new Date();
    document.getElementById('input-slip-no').value = '';
    document.getElementById('input-dhobi-name').value = state.defaultDhobiName || '';
    document.getElementById('input-amount').value = '';
    document.getElementById('input-return-date').value = '';
    document.getElementById('input-notes').value = '';

    resetChitPreviewUI();
    renderAddClothingRows();
    updateLiveTotal();
    navigateTo('screen-add');
  }

  function generateNextRecordId() {
    const now = new Date();
    const yy = String(now.getFullYear()).slice(-2);
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const count = String(state.records.length + 1).padStart(3, '0');
    return `DT-${yy}${mm}${dd}-${count}`;
  }

  function renderAddClothingRows() {
    const container = document.getElementById('clothing-rows-list');
    container.innerHTML = '';

    const activeCategories = state.categories.filter(c => c.active !== false);

    activeCategories.forEach(cat => {
      const row = document.createElement('div');
      row.className = 'clothing-row';
      const qty = state.addQuantities[cat.name] || 0;

      row.innerHTML = `
        <div class="clothing-icon-badge">👕</div>
        <span class="clothing-name">${cat.name}</span>
        <div class="stepper-control">
          <button class="stepper-btn minus" data-cat="${cat.name}">−</button>
          <div class="stepper-val ${qty > 0 ? 'active' : ''}" data-cat="${cat.name}">${qty}</div>
          <button class="stepper-btn plus" data-cat="${cat.name}">+</button>
        </div>
      `;

      // Plus / Minus actions
      row.querySelector('.minus').addEventListener('click', () => {
        const cur = state.addQuantities[cat.name] || 0;
        if (cur > 0) {
          state.addQuantities[cat.name] = cur - 1;
          renderAddClothingRows();
          updateLiveTotal();
        }
      });

      row.querySelector('.plus').addEventListener('click', () => {
        const cur = state.addQuantities[cat.name] || 0;
        state.addQuantities[cat.name] = cur + 1;
        renderAddClothingRows();
        updateLiveTotal();
      });

      // Direct Click on number
      row.querySelector('.stepper-val').addEventListener('click', () => {
        const input = prompt(`Enter quantity for ${cat.name}:`, qty);
        if (input !== null) {
          const num = parseInt(input, 10);
          if (!isNaN(num) && num >= 0) {
            state.addQuantities[cat.name] = num;
            renderAddClothingRows();
            updateLiveTotal();
          }
        }
      });

      container.appendChild(row);
    });
  }

  function updateLiveTotal() {
    const total = Object.values(state.addQuantities).reduce((a, b) => a + b, 0);
    document.getElementById('live-total-pill').textContent = `Total: ${total}`;
    document.getElementById('btn-save-record').textContent = `Save Bag (${total} items)`;
  }

  function resetChitPreviewUI() {
    const placeholder = document.getElementById('chit-empty-placeholder');
    const img = document.getElementById('chit-preview-img');
    const removeBtn = document.getElementById('btn-remove-chit');
    const ocrBar = document.getElementById('ocr-action-bar');

    if (state.tempChitImage) {
      img.src = state.tempChitImage;
      img.classList.remove('hidden');
      placeholder.classList.add('hidden');
      removeBtn.classList.remove('hidden');
      ocrBar.classList.remove('hidden');
    } else {
      img.src = '';
      img.classList.add('hidden');
      placeholder.classList.remove('hidden');
      removeBtn.classList.add('hidden');
      ocrBar.classList.add('hidden');
    }
  }

  // ==================== SCREEN 3: OCR SCANNING ====================
  function triggerOcrScan() {
    if (!state.tempChitImage) return;

    navigateTo('screen-ocr');
    document.getElementById('ocr-source-image').src = state.tempChitImage;

    // Simulate smart local ML Kit text recognition
    setTimeout(() => {
      simulateOcrExtraction();
    }, 400);
  }

  function simulateOcrExtraction() {
    // Simulated recognition logic
    const detectedSlip = "1042";
    const detectedDate = new Date().toISOString().split('T')[0];
    const detectedTotal = 7;

    state.ocrQuantities = {
      "Shirt": 2,
      "T-Shirt": 3,
      "Pant": 1,
      "Towel": 1
    };

    document.getElementById('ocr-slip-number').value = detectedSlip;
    document.getElementById('ocr-detected-date').value = detectedDate;

    renderOcrItems();
    checkOcrTotalMismatch(detectedTotal);
  }

  function renderOcrItems() {
    const container = document.getElementById('ocr-items-list');
    container.innerHTML = '';
    let sum = 0;

    Object.entries(state.ocrQuantities).forEach(([cat, count]) => {
      sum += count;
      const row = document.createElement('div');
      row.className = 'clothing-row';
      row.innerHTML = `
        <div class="clothing-icon-badge">👕</div>
        <span class="clothing-name">${cat}</span>
        <div class="stepper-control">
          <button class="stepper-btn minus" data-cat="${cat}">−</button>
          <div class="stepper-val active">${count}</div>
          <button class="stepper-btn plus" data-cat="${cat}">+</button>
        </div>
      `;

      row.querySelector('.minus').addEventListener('click', () => {
        if (state.ocrQuantities[cat] > 0) {
          state.ocrQuantities[cat]--;
          if (state.ocrQuantities[cat] === 0) delete state.ocrQuantities[cat];
          renderOcrItems();
        }
      });

      row.querySelector('.plus').addEventListener('click', () => {
        state.ocrQuantities[cat]++;
        renderOcrItems();
      });

      container.appendChild(row);
    });

    document.getElementById('ocr-current-sum').textContent = `Sum: ${sum}`;
  }

  function checkOcrTotalMismatch(expectedTotal) {
    const currentSum = Object.values(state.ocrQuantities).reduce((a, b) => a + b, 0);
    const banner = document.getElementById('ocr-mismatch-banner');
    const bannerText = document.getElementById('ocr-mismatch-text');

    if (expectedTotal && currentSum !== expectedTotal) {
      bannerText.textContent = `Recognized item sum (${currentSum}) differs from detected chit total (${expectedTotal}). Please verify numbers below.`;
      banner.classList.remove('hidden');
    } else {
      banner.classList.add('hidden');
    }
  }

  function applyOcrToRecord() {
    const slip = document.getElementById('ocr-slip-number').value;
    if (slip) document.getElementById('input-slip-no').value = slip;

    Object.entries(state.ocrQuantities).forEach(([cat, count]) => {
      if (count > 0) state.addQuantities[cat] = count;
    });

    renderAddClothingRows();
    updateLiveTotal();
    navigateTo('screen-add');
  }

  // ==================== SCREEN 4: RECORD DETAILS ====================
  function openRecordDetails(recordId) {
    state.currentRecordId = recordId;
    navigateTo('screen-details');
  }

  function renderDetailsScreen() {
    const record = state.records.find(r => r.id === state.currentRecordId);
    if (!record) {
      navigateTo('screen-home');
      return;
    }

    document.getElementById('details-record-id').textContent = record.id;
    document.getElementById('details-date-subtitle').textContent = formatDateDisplay(record.dateGiven);

    const status = computeRecordStatus(record);
    const statusPill = document.getElementById('details-status-badge');
    statusPill.className = `status-pill ${status === 'PENDING' ? 'status-pending' : (status === 'COLLECTED' ? 'status-collected' : 'status-partial')}`;
    statusPill.textContent = status === 'PENDING' ? 'Pending' : (status === 'COLLECTED' ? 'Collected' : 'Partially Returned');

    const totalGiven = record.items.reduce((sum, i) => sum + i.quantityGiven, 0);
    const totalReturned = record.items.reduce((sum, i) => sum + i.quantityReturned, 0);
    const totalPending = Math.max(0, totalGiven - totalReturned);

    document.getElementById('details-count-given').textContent = totalGiven;
    document.getElementById('details-count-returned').textContent = totalReturned;
    document.getElementById('details-count-pending').textContent = totalPending;

    // Discrepancy notice
    const discNotice = document.getElementById('details-discrepancy-notice');
    if (record.forceCollected && totalPending > 0) {
      discNotice.classList.remove('hidden');
    } else {
      discNotice.classList.add('hidden');
    }

    // Chit Image card
    const chitImg = document.getElementById('details-chit-img');
    const chitCard = document.getElementById('details-chit-card');
    if (record.chitImage) {
      chitImg.src = record.chitImage;
      chitCard.classList.remove('hidden');
    } else {
      chitCard.classList.add('hidden');
    }

    // Metadata
    document.getElementById('details-val-slip').textContent = record.slipNumber || 'Not recorded';
    document.getElementById('details-val-dhobi').textContent = record.dhobiName || 'Not specified';
    document.getElementById('details-val-amount').textContent = record.amount ? `₹${parseFloat(record.amount).toFixed(2)}` : 'Not specified';
    document.getElementById('details-val-expected').textContent = record.expectedReturnDate ? formatDateDisplay(record.expectedReturnDate) : 'Not set';

    const notesRow = document.getElementById('details-notes-row');
    if (record.notes) {
      document.getElementById('details-val-notes').textContent = record.notes;
      notesRow.classList.remove('hidden');
    } else {
      notesRow.classList.add('hidden');
    }

    // Inventory Table
    const tableBody = document.getElementById('details-items-table-body');
    tableBody.innerHTML = '';

    record.items.forEach(item => {
      const pending = Math.max(0, item.quantityGiven - item.quantityReturned);
      const row = document.createElement('div');
      row.className = 'table-row';
      row.innerHTML = `
        <span class="col-item">${item.categoryName}</span>
        <span class="col-qty">${item.quantityGiven}</span>
        <span class="col-qty ${item.quantityReturned > 0 ? 'green-text' : ''}">${item.quantityReturned}</span>
        <span class="col-qty ${pending > 0 ? 'danger-text' : ''}">${pending}</span>
      `;
      tableBody.appendChild(row);
    });
  }

  // ==================== SCREEN 5: HISTORY ====================
  function renderHistoryScreen() {
    const container = document.getElementById('history-records-list');
    container.innerHTML = '';

    let filtered = [...state.records];

    // Status filter
    if (state.filterStatus !== 'ALL') {
      filtered = filtered.filter(r => computeRecordStatus(r) === state.filterStatus);
    }

    // Search query filter
    if (state.searchQuery.trim()) {
      const q = state.searchQuery.toLowerCase();
      filtered = filtered.filter(r =>
        (r.id && r.id.toLowerCase().includes(q)) ||
        (r.slipNumber && r.slipNumber.toLowerCase().includes(q)) ||
        (r.dhobiName && r.dhobiName.toLowerCase().includes(q)) ||
        (r.notes && r.notes.toLowerCase().includes(q))
      );
    }

    // Date filter
    if (state.filterDate === '7DAYS') {
      const weekAgo = new Date();
      weekAgo.setDate(weekAgo.getDate() - 7);
      filtered = filtered.filter(r => new Date(r.dateGiven) >= weekAgo);
    } else if (state.filterDate === 'MONTH') {
      const firstDay = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
      filtered = filtered.filter(r => new Date(r.dateGiven) >= firstDay);
    }

    // Sort newest first
    filtered.sort((a, b) => b.createdAt - a.createdAt);

    document.getElementById('history-total-count').textContent = `${filtered.length} record${filtered.length === 1 ? '' : 's'} logged`;

    if (filtered.length === 0) {
      container.innerHTML = `
        <div class="ui-card empty-state">
          <div class="empty-icon">🔍</div>
          <h4>No Records Match</h4>
          <p>No laundry bags match your search or filter filters.</p>
        </div>
      `;
      return;
    }

    filtered.forEach(record => {
      container.appendChild(createRecordCardElement(record));
    });
  }

  // ==================== SCREEN 6: SETTINGS ====================
  function renderSettingsScreen() {
    document.getElementById('setting-default-dhobi').value = state.defaultDhobiName || '';

    const list = document.getElementById('settings-categories-list');
    list.innerHTML = '';

    state.categories.forEach((cat, index) => {
      const item = document.createElement('div');
      item.className = 'category-toggle-item';
      item.innerHTML = `
        <div>
          <strong>${cat.name}</strong>
          <span style="font-size: 11px; color: var(--text-secondary); margin-left: 6px;">
            ${cat.default ? '(Default)' : '(Custom)'}
          </span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          ${!cat.default ? `<button class="secondary-btn danger-text btn-del-cat" data-idx="${index}">Delete</button>` : ''}
          <input type="checkbox" class="cat-toggle-checkbox" data-idx="${index}" ${cat.active !== false ? 'checked' : ''}>
        </div>
      `;

      item.querySelector('.cat-toggle-checkbox').addEventListener('change', (e) => {
        state.categories[index].active = e.target.checked;
        saveToLocalStorage();
      });

      const delBtn = item.querySelector('.btn-del-cat');
      if (delBtn) {
        delBtn.addEventListener('click', () => {
          if (confirm(`Delete custom category '${cat.name}'?`)) {
            state.categories.splice(index, 1);
            saveToLocalStorage();
            renderSettingsScreen();
          }
        });
      }

      list.appendChild(item);
    });
  }

  // ==================== COLLECTION TRACKING MODAL ====================
  function openCollectionModal() {
    const record = state.records.find(r => r.id === state.currentRecordId);
    if (!record) return;

    const container = document.getElementById('collection-items-list');
    container.innerHTML = '';

    const localReturnCounts = {};
    record.items.forEach(i => {
      localReturnCounts[i.categoryName] = i.quantityReturned;
    });

    record.items.forEach(item => {
      const row = document.createElement('div');
      row.className = 'clothing-row';
      const curReturned = localReturnCounts[item.categoryName];

      row.innerHTML = `
        <div class="clothing-info-group">
          <strong>${item.categoryName}</strong>
          <div style="font-size: 11px; color: var(--text-secondary)">Given: ${item.quantityGiven} • Pending: <span class="row-pending">${item.quantityGiven - curReturned}</span></div>
        </div>
        <div class="stepper-control" style="margin-left: auto;">
          <button class="stepper-btn minus">−</button>
          <div class="stepper-val active">${curReturned}</div>
          <button class="stepper-btn plus">+</button>
        </div>
      `;

      row.querySelector('.minus').addEventListener('click', () => {
        if (localReturnCounts[item.categoryName] > 0) {
          localReturnCounts[item.categoryName]--;
          row.querySelector('.stepper-val').textContent = localReturnCounts[item.categoryName];
          row.querySelector('.row-pending').textContent = item.quantityGiven - localReturnCounts[item.categoryName];
        }
      });

      row.querySelector('.plus').addEventListener('click', () => {
        if (localReturnCounts[item.categoryName] < item.quantityGiven) {
          localReturnCounts[item.categoryName]++;
          row.querySelector('.stepper-val').textContent = localReturnCounts[item.categoryName];
          row.querySelector('.row-pending').textContent = item.quantityGiven - localReturnCounts[item.categoryName];
        }
      });

      container.appendChild(row);
    });

    document.getElementById('btn-collection-mark-all').onclick = () => {
      record.items.forEach(i => {
        localReturnCounts[i.categoryName] = i.quantityGiven;
      });
      container.querySelectorAll('.stepper-val').forEach((valEl, idx) => {
        valEl.textContent = record.items[idx].quantityGiven;
      });
      container.querySelectorAll('.row-pending').forEach((pendEl) => {
        pendEl.textContent = '0';
      });
    };

    document.getElementById('btn-save-collection').onclick = () => {
      let missingCount = 0;
      record.items.forEach(i => {
        i.quantityReturned = localReturnCounts[i.categoryName] || 0;
        if (i.quantityGiven > i.quantityReturned) {
          missingCount += (i.quantityGiven - i.quantityReturned);
        }
      });

      if (missingCount > 0) {
        if (confirm(`There are still ${missingCount} item(s) pending. Do you want to save this progress as Partially Returned?`)) {
          record.forceCollected = false;
          saveToLocalStorage();
          closeModal('modal-collection');
          renderDetailsScreen();
        }
      } else {
        record.forceCollected = false;
        saveToLocalStorage();
        closeModal('modal-collection');
        renderDetailsScreen();
      }
    };

    openModal('modal-collection');
  }

  // ==================== WHATSAPP SHARE GENERATOR ====================
  function openWhatsAppShareModal() {
    const record = state.records.find(r => r.id === state.currentRecordId);
    if (!record) return;

    let msg = `🧺 *Laundry Bag:* ${record.id}\n`;
    msg += `📅 *Date:* ${formatDateDisplay(record.dateGiven)}\n`;
    if (record.slipNumber) msg += `🔢 *Slip No.:* ${record.slipNumber}\n`;
    if (record.dhobiName) msg += `🏪 *Shop/Dhobi:* ${record.dhobiName}\n`;
    if (record.amount) msg += `💰 *Amount:* ₹${parseFloat(record.amount).toFixed(2)}\n`;

    msg += `\n*Clothing Inventory:*\n`;
    let total = 0;
    record.items.forEach(item => {
      if (item.quantityGiven > 0) {
        msg += `• ${item.categoryName}: ${item.quantityGiven}\n`;
        total += item.quantityGiven;
      }
    });

    msg += `\n*Total:* ${total} items\n`;
    if (record.notes) msg += `📝 *Notes:* ${record.notes}\n`;
    msg += `\nPlease confirm the count. Thank you!`;

    document.getElementById('whatsapp-message-preview').textContent = msg;

    document.getElementById('btn-copy-whatsapp').onclick = () => {
      navigator.clipboard.writeText(msg).then(() => {
        alert("Inventory message copied to clipboard! Ready to paste into WhatsApp.");
      }).catch(() => {
        alert("Could not copy automatically. You can copy the text manually.");
      });
    };

    openModal('modal-whatsapp');
  }

  // ==================== SAMPLE DATA GENERATOR ====================
  function loadSampleData(confirmOverwrite = true) {
    if (confirmOverwrite && state.records.length > 0) {
      if (!confirm("This will replace current data with sample laundry bags. Proceed?")) return;
    }

    const sampleChitBase64 = createSampleChitSvg();

    const sample1 = {
      id: "DT-261020-001",
      dateGiven: new Date().toISOString().split('T')[0],
      slipNumber: "1042",
      dhobiName: "Raju Dhobi",
      amount: 140,
      expectedReturnDate: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0],
      notes: "Starch white shirts, gentle wash on kurta",
      chitImage: sampleChitBase64,
      createdAt: Date.now() - 3600000,
      forceCollected: false,
      items: [
        { categoryName: "Shirt", quantityGiven: 2, quantityReturned: 0 },
        { categoryName: "T-Shirt", quantityGiven: 3, quantityReturned: 0 },
        { categoryName: "Pant", quantityGiven: 1, quantityReturned: 0 },
        { categoryName: "Towel", quantityGiven: 1, quantityReturned: 0 }
      ]
    };

    const sample2 = {
      id: "DT-261018-002",
      dateGiven: new Date(Date.now() - 86400000 * 2).toISOString().split('T')[0],
      slipNumber: "B-88",
      dhobiName: "Shree Krishna Drycleaners",
      amount: 220,
      expectedReturnDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      notes: "Heavy pressing for pajamas",
      chitImage: sampleChitBase64,
      createdAt: Date.now() - 86400000 * 2,
      forceCollected: false,
      items: [
        { categoryName: "Kurta", quantityGiven: 2, quantityReturned: 2 },
        { categoryName: "Pajama", quantityGiven: 2, quantityReturned: 1 },
        { categoryName: "Bedsheet", quantityGiven: 1, quantityReturned: 1 }
      ]
    };

    const sample3 = {
      id: "DT-261012-003",
      dateGiven: new Date(Date.now() - 86400000 * 8).toISOString().split('T')[0],
      slipNumber: "512",
      dhobiName: "Raju Dhobi",
      amount: 80,
      expectedReturnDate: new Date(Date.now() - 86400000 * 5).toISOString().split('T')[0],
      notes: "Returned on time",
      chitImage: sampleChitBase64,
      createdAt: Date.now() - 86400000 * 8,
      forceCollected: false,
      items: [
        { categoryName: "Pant", quantityGiven: 2, quantityReturned: 2 },
        { categoryName: "Shirt", quantityGiven: 2, quantityReturned: 2 }
      ]
    };

    state.records = [sample1, sample2, sample3];
    saveToLocalStorage();
    renderCurrentScreen();
  }

  function createSampleChitSvg() {
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300">
        <rect width="400" height="300" fill="#FFFDF8" stroke="#D1D5DB" stroke-width="2"/>
        <line x1="20" y1="50" x2="380" y2="50" stroke="#E5E7EB" stroke-width="2"/>
        <text x="30" y="38" font-family="sans-serif" font-size="18" font-weight="bold" fill="#1F2937">RAJU DHOBI &amp; PRESS</text>
        <text x="280" y="36" font-family="sans-serif" font-size="14" fill="#4B5563">SLIP #1042</text>
        <text x="30" y="80" font-family="sans-serif" font-size="14" fill="#374151">Shirt - 2</text>
        <text x="30" y="110" font-family="sans-serif" font-size="14" fill="#374151">T-Shirt - 3</text>
        <text x="30" y="140" font-family="sans-serif" font-size="14" fill="#374151">Pant - 1</text>
        <text x="30" y="170" font-family="sans-serif" font-size="14" fill="#374151">Towel - 1</text>
        <line x1="20" y1="200" x2="380" y2="200" stroke="#9CA3AF" stroke-width="1.5" stroke-dasharray="4"/>
        <text x="30" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#111827">TOTAL: 7 items</text>
        <text x="260" y="230" font-family="sans-serif" font-size="15" font-weight="bold" fill="#047857">Amount: ₹140</text>
        <text x="30" y="270" font-family="sans-serif" font-size="12" fill="#6B7280">Date: 20-10-2026 • Return in 3 days</text>
      </svg>
    `;
    return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
  }

  // ==================== HELPERS & EVENT LISTENERS ====================
  function setupEventListeners() {
    // Save record button
    document.getElementById('btn-save-record').addEventListener('click', saveCurrentRecord);

    // Photo uploads
    document.getElementById('btn-sample-chit').addEventListener('click', () => {
      state.tempChitImage = createSampleChitSvg();
      resetChitPreviewUI();
    });

    document.getElementById('btn-mock-camera').addEventListener('click', () => {
      document.getElementById('chit-file-input').click();
    });

    document.getElementById('chit-file-input').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (evt) => {
          state.tempChitImage = evt.target.result;
          resetChitPreviewUI();
        };
        reader.readAsDataURL(file);
      }
    });

    document.getElementById('btn-remove-chit').addEventListener('click', () => {
      state.tempChitImage = null;
      resetChitPreviewUI();
    });

    // OCR trigger
    document.getElementById('btn-scan-ocr').addEventListener('click', triggerOcrScan);
    document.getElementById('btn-apply-ocr').addEventListener('click', applyOcrToRecord);

    // Custom Category in Add screen
    document.getElementById('btn-add-custom-cat').addEventListener('click', () => {
      const name = prompt("Enter custom clothing item name (e.g. Scarf, Blanket, Blazer):");
      if (name && name.trim()) {
        const trimmed = name.trim();
        if (!state.categories.some(c => c.name.toLowerCase() === trimmed.toLowerCase())) {
          state.categories.push({ name: trimmed, active: true, default: false });
          saveToLocalStorage();
        }
        state.addQuantities[trimmed] = 1;
        renderAddClothingRows();
        updateLiveTotal();
      }
    });

    // Details actions
    document.getElementById('btn-open-collection').addEventListener('click', openCollectionModal);
    document.getElementById('btn-whatsapp-share').addEventListener('click', openWhatsAppShareModal);
    document.getElementById('btn-open-fullscreen-chit').addEventListener('click', () => {
      const record = state.records.find(r => r.id === state.currentRecordId);
      if (record && record.chitImage) {
        document.getElementById('fs-chit-img').src = record.chitImage;
        openModal('modal-fullscreen-chit');
      }
    });
    document.getElementById('details-chit-preview-box').addEventListener('click', () => {
      const record = state.records.find(r => r.id === state.currentRecordId);
      if (record && record.chitImage) {
        document.getElementById('fs-chit-img').src = record.chitImage;
        openModal('modal-fullscreen-chit');
      }
    });

    document.getElementById('btn-quick-mark-all').addEventListener('click', () => {
      const record = state.records.find(r => r.id === state.currentRecordId);
      if (record) {
        record.items.forEach(i => i.quantityReturned = i.quantityGiven);
        record.forceCollected = false;
        saveToLocalStorage();
        renderDetailsScreen();
      }
    });

    document.getElementById('btn-delete-details').addEventListener('click', () => {
      if (confirm(`Delete laundry bag ${state.currentRecordId}? This cannot be undone.`)) {
        state.records = state.records.filter(r => r.id !== state.currentRecordId);
        saveToLocalStorage();
        navigateTo('screen-home');
      }
    });

    // History filters
    document.querySelectorAll('.filter-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        state.filterStatus = chip.dataset.status;
        renderHistoryScreen();
      });
    });

    document.querySelectorAll('.date-chip').forEach(chip => {
      chip.addEventListener('click', (e) => {
        document.querySelectorAll('.date-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        state.filterDate = chip.dataset.date;
        renderHistoryScreen();
      });
    });

    const searchInput = document.getElementById('history-search-input');
    const clearSearch = document.getElementById('btn-clear-search');
    searchInput.addEventListener('input', (e) => {
      state.searchQuery = e.target.value;
      clearSearch.classList.toggle('hidden', !e.target.value);
      renderHistoryScreen();
    });

    clearSearch.addEventListener('click', () => {
      searchInput.value = '';
      state.searchQuery = '';
      clearSearch.classList.add('hidden');
      renderHistoryScreen();
    });

    // Settings actions
    document.getElementById('setting-default-dhobi').addEventListener('change', (e) => {
      state.defaultDhobiName = e.target.value.trim();
      saveToLocalStorage();
    });

    document.getElementById('btn-setting-add-cat').addEventListener('click', () => {
      const name = prompt("Enter new category name:");
      if (name && name.trim()) {
        state.categories.push({ name: name.trim(), active: true, default: false });
        saveToLocalStorage();
        renderSettingsScreen();
      }
    });

    // Backup Export
    document.getElementById('btn-export-backup').addEventListener('click', () => {
      const backupData = {
        app: "Dhobi Tracker",
        version: "1.0.0",
        timestamp: Date.now(),
        records: state.records,
        categories: state.categories,
        defaultDhobiName: state.defaultDhobiName
      };

      const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `DhobiTracker_Backup_${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
    });

    // Backup Import
    document.getElementById('btn-import-backup-trigger').addEventListener('click', () => {
      document.getElementById('backup-file-input').click();
    });

    document.getElementById('backup-file-input').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (evt) => {
          try {
            const data = JSON.parse(evt.target.result);
            if (data.records && Array.isArray(data.records)) {
              state.records = data.records;
              if (data.categories) state.categories = data.categories;
              if (data.defaultDhobiName) state.defaultDhobiName = data.defaultDhobiName;
              saveToLocalStorage();
              alert(`Backup restored successfully: ${data.records.length} records imported.`);
              renderCurrentScreen();
            } else {
              alert("Invalid backup file format.");
            }
          } catch (err) {
            alert("Error parsing backup file: " + err.message);
          }
        };
        reader.readAsText(file);
      }
    });

    // Desktop Toolbar sample data & reset
    document.getElementById('btn-seed-sample').addEventListener('click', () => loadSampleData(true));
    document.getElementById('btn-reset-data').addEventListener('click', () => {
      if (confirm("Reset and clear all saved laundry records and categories?")) {
        localStorage.clear();
        state.records = [];
        state.categories = DEFAULT_CATEGORIES;
        saveToLocalStorage();
        renderCurrentScreen();
      }
    });

    // Modal Close buttons
    document.getElementById('btn-close-collection').addEventListener('click', () => closeModal('modal-collection'));
    document.getElementById('btn-close-fullscreen-chit').addEventListener('click', () => closeModal('modal-fullscreen-chit'));
    document.getElementById('btn-close-whatsapp').addEventListener('click', () => closeModal('modal-whatsapp'));
  }

  function saveCurrentRecord() {
    const totalItems = Object.values(state.addQuantities).reduce((a, b) => a + b, 0);
    if (totalItems <= 0) {
      alert("Please specify quantity for at least one clothing item before saving.");
      return;
    }

    const items = Object.entries(state.addQuantities)
      .filter(([_, qty]) => qty > 0)
      .map(([cat, qty]) => ({
        categoryName: cat,
        quantityGiven: qty,
        quantityReturned: 0
      }));

    const newRecord = {
      id: document.getElementById('add-record-id-badge').textContent,
      dateGiven: document.getElementById('input-date-given').value,
      slipNumber: document.getElementById('input-slip-no').value.trim() || null,
      dhobiName: document.getElementById('input-dhobi-name').value.trim() || null,
      amount: parseFloat(document.getElementById('input-amount').value) || null,
      expectedReturnDate: document.getElementById('input-return-date').value || null,
      notes: document.getElementById('input-notes').value.trim() || null,
      chitImage: state.tempChitImage,
      createdAt: Date.now(),
      forceCollected: false,
      items: items
    };

    state.records.unshift(newRecord);
    saveToLocalStorage();
    navigateTo('screen-home');
  }

  function openModal(modalId) {
    document.getElementById(modalId).classList.remove('hidden');
  }

  function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
  }

  function formatDateDisplay(dateStr) {
    if (!dateStr) return 'Not set';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' });
    } catch (_) {
      return dateStr;
    }
  }

  // Start app
  window.addEventListener('DOMContentLoaded', init);
})();
