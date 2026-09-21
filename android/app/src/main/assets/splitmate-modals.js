/**
 * SplitMate v2.0 — Spring-Animated Modals:
 * 1. User Profile & Illustrated Character Art Picker (Open-Peeps & Micah + DiceBear API)
 * 2. 161 World Currencies Searchable Picker (REST Countries + Open Exchange Rates)
 * 3. Create New Group Modal
 * 4. Add Group Member Modal
 * 5. Quick Expense Logger & Calculator Modal (0.00 Rounding Drift)
 */

function closeModal() {
  const m = document.getElementById('app-modal-root');
  if (m) m.innerHTML = '';
}

// ============================================================================
// 1. PROFILE & ILLUSTRATED CHARACTER ART MODAL
// ============================================================================
let tempProfileAvatar = '';

function openProfileModal() {
  haptic('tap');
  tempProfileAvatar = STATE.user.avatar || 'avatars/peep_1.svg';
  const root = document.getElementById('app-modal-root');
  if (!root) return;

  root.innerHTML = `
    <div class="fixed inset-0 z-50 bg-black/45 backdrop-blur-xs flex items-end justify-center" onclick="if(event.target===this && STATE.onboarded) closeModal();">
      <div class="w-full max-w-md bg-[#FAF6F0] rounded-t-[32px] p-6 border-t border-[#EDE7DF] shadow-2xl space-y-5 max-h-[92vh] overflow-y-auto animate-slide-up">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2.5">
            <img src="app_icon.svg" alt="SplitMate" class="w-9 h-9 rounded-xl shadow-2xs"/>
            <div>
              <h2 class="text-[18px] font-extrabold text-[#23201E] leading-tight">Your Profile & Currency</h2>
              <p class="text-[11px] font-semibold text-[#6E675F]">Stored privately on your phone</p>
            </div>
          </div>
          ${STATE.onboarded ? `<button onclick="closeModal()" class="w-8 h-8 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] font-bold">✕</button>` : ''}
        </div>

        <!-- Live Character Preview & Name Input -->
        <div class="bg-white rounded-[24px] p-4 border border-[#EDE7DF] flex items-center gap-3.5">
          <div id="profile-avatar-preview" class="shrink-0">
            ${renderAvatarImg(tempProfileAvatar, STATE.user.name, 'w-16 h-16')}
          </div>
          <div class="flex-1 min-w-0 space-y-1">
            <label class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Your Name</label>
            <input id="profile-name-input" type="text" value="${escapeHtml(STATE.user.name)}" placeholder="Enter your name" class="w-full h-10 px-3 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[15px] font-extrabold text-[#23201E] focus:outline-none focus:border-[#23201E]"/>
          </div>
        </div>

        <!-- Illustrated Face & Upper-Body Character Art Picker -->
        <div class="bg-white rounded-[24px] p-4 border border-[#EDE7DF] space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-[12px] font-extrabold text-[#23201E]">Choose Character Art</span>
            <button onclick="generateRandomCharacterArt()" class="h-7 px-2.5 rounded-full bg-[#D7E8B6] text-[#2B4810] text-[11px] font-extrabold flex items-center gap-1 active:scale-95">
              <span>🎲 Generate New Face/Body</span>
            </button>
          </div>
          <div class="grid grid-cols-6 gap-2">
            ${AVATAR_GALLERY.map(av => `
              <button onclick="selectProfileAvatar('${av.url}')" class="p-1 rounded-2xl border-2 transition-all flex items-center justify-center ${tempProfileAvatar === av.url ? 'border-[#23201E] bg-[#D7E8B6]/40 scale-105' : 'border-transparent bg-[#FAF6F0]'}">
                <img src="${av.url}" alt="${av.label}" class="w-11 h-11 rounded-full object-cover"/>
              </button>
            `).join('')}
          </div>
        </div>

        <!-- Default Currency Button (161 World Currencies) -->
        <div class="bg-white rounded-[24px] p-4 border border-[#EDE7DF] flex items-center justify-between">
          <div>
            <div class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Default Currency</div>
            <div class="text-[14px] font-extrabold text-[#23201E] mt-0.5">${STATE.user.currencyFlag || '🇮🇳'} ${escapeHtml(STATE.user.currencyCode)} · ${escapeHtml(STATE.user.currencyName)} (${escapeHtml(STATE.user.currencySymbol)})</div>
          </div>
          <button onclick="openCurrencyModal(true)" class="h-9 px-3.5 rounded-full bg-[#FAF6F0] border border-[#EDE7DF] text-[12px] font-extrabold text-[#23201E]">
            Change (161) ▾
          </button>
        </div>

        <button onclick="saveProfileModal()" class="w-full h-13 py-3.5 rounded-[20px] bg-[#23201E] text-white text-[15px] font-extrabold shadow-md active:scale-95 transition-all">
          Save & Continue →
        </button>
      </div>
    </div>
  `;
}

function selectProfileAvatar(url) {
  haptic('tap');
  tempProfileAvatar = url;
  const prev = document.getElementById('profile-avatar-preview');
  if (prev) prev.innerHTML = renderAvatarImg(tempProfileAvatar, STATE.user.name, 'w-16 h-16');
}

function generateRandomCharacterArt() {
  haptic('tap');
  const nameInput = document.getElementById('profile-name-input');
  const seed = ((nameInput && nameInput.value) || 'User') + '_' + Math.floor(Math.random() * 9999);
  const styles = ['open-peeps', 'micah'];
  const style = styles[Math.floor(Math.random() * styles.length)];
  tempProfileAvatar = getDiceBearUrl(seed, style);
  const prev = document.getElementById('profile-avatar-preview');
  if (prev) prev.innerHTML = renderAvatarImg(tempProfileAvatar, seed, 'w-16 h-16');
}

function saveProfileModal() {
  haptic('confirm');
  const nameEl = document.getElementById('profile-name-input');
  const newName = (nameEl && nameEl.value.trim()) || 'Akshay';
  STATE.user.name = newName;
  STATE.user.avatar = tempProfileAvatar || STATE.user.avatar;
  STATE.onboarded = true;

  // Sync user's name and avatar across all group member records for `m_me`
  STATE.groups.forEach(g => {
    g.members.forEach(m => {
      if (m.isMe || m.id === 'm_me') {
        m.name = newName;
        m.avatar = STATE.user.avatar;
      }
    });
  });

  try {
    if (window.SplitMateNative && window.SplitMateNative.saveUserProfile) {
      window.SplitMateNative.saveUserProfile(newName, '🌱', STATE.user.currencyCode, true);
    }
  } catch (e) {}

  closeModal();
  renderApp();
  showToast(`Profile updated for ${newName}`);
}

// ============================================================================
// 2. SEARCHABLE 161 WORLD CURRENCIES MODAL (REST Countries + Open ER API)
// ============================================================================
function openCurrencyModal(returnToProfile = false) {
  haptic('tap');
  const root = document.getElementById('app-modal-root');
  if (!root) return;

  root.innerHTML = `
    <div class="fixed inset-0 z-50 bg-black/45 backdrop-blur-xs flex items-end justify-center" onclick="if(event.target===this) closeModal();">
      <div class="w-full max-w-md bg-[#FAF6F0] rounded-t-[32px] p-5 border-t border-[#EDE7DF] shadow-2xl flex flex-col h-[82vh] animate-slide-up">
        <div class="flex items-center justify-between pb-3">
          <div>
            <h2 class="text-[17px] font-extrabold text-[#23201E]">Select World Currency (${WORLD_CURRENCIES.length})</h2>
            <p class="text-[11px] font-semibold text-[#6E675F]">Search by country, currency name, code, or symbol</p>
          </div>
          <button onclick="${returnToProfile ? 'openProfileModal()' : 'closeModal()'}" class="w-8 h-8 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] font-bold">✕</button>
        </div>

        <input id="currency-search-input" type="text" oninput="filterCurrencyList(this.value, ${returnToProfile})" placeholder="Search India, INR, ₹, USD, Euro, Yen..." class="w-full h-11 px-4 rounded-2xl bg-white border border-[#EDE7DF] text-[13px] font-bold text-[#23201E] mb-3 focus:outline-none focus:border-[#23201E]"/>

        <div id="currency-list-container" class="flex-1 overflow-y-auto space-y-1.5 pr-1">
          ${renderCurrencyRows('', returnToProfile)}
        </div>
      </div>
    </div>
  `;
}

function renderCurrencyRows(query, returnToProfile) {
  const q = (query || '').trim().toLowerCase();
  const matches = WORLD_CURRENCIES.filter(c =>
    !q ||
    c.code.toLowerCase().includes(q) ||
    c.name.toLowerCase().includes(q) ||
    c.country.toLowerCase().includes(q) ||
    c.symbol.toLowerCase().includes(q)
  );

  return matches.map(c => {
    const selected = STATE.user.currencyCode === c.code;
    return `
      <button onclick="applySelectedCurrency('${c.code}', ${returnToProfile})" class="w-full p-3 rounded-2xl border text-left flex items-center justify-between transition-all ${selected ? 'bg-[#D7E8B6] border-[#365314]' : 'bg-white border-[#EDE7DF] hover:bg-[#FAF6F0]'}">
        <div class="flex items-center gap-3 min-w-0">
          <span class="text-[22px] shrink-0">${c.flag || '🏳️'}</span>
          <div class="min-w-0">
            <div class="text-[13px] font-extrabold text-[#23201E] truncate">${escapeHtml(c.code)} · ${escapeHtml(c.name)}</div>
            <div class="text-[11px] font-semibold text-[#6E675F] truncate">${escapeHtml(c.country)} ${c.rateFromUSD ? `· 1 USD = ${Number(c.rateFromUSD).toFixed(2)} ${escapeHtml(c.code)}` : ''}</div>
          </div>
        </div>
        <span class="px-2.5 py-1 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[13px] font-extrabold text-[#23201E] shrink-0">${escapeHtml(c.symbol)}</span>
      </button>
    `;
  }).join('');
}

function filterCurrencyList(val, returnToProfile) {
  const box = document.getElementById('currency-list-container');
  if (box) box.innerHTML = renderCurrencyRows(val, returnToProfile);
}

function applySelectedCurrency(code, returnToProfile) {
  haptic('confirm');
  const curr = WORLD_CURRENCIES.find(c => c.code === code);
  if (!curr) return;
  STATE.user.currencyCode = curr.code;
  STATE.user.currencySymbol = curr.symbol;
  STATE.user.currencyName = curr.name;
  STATE.user.currencyFlag = curr.flag;

  // Update all existing groups to reflect the selected currency
  STATE.groups.forEach(g => {
    g.currencyCode = curr.code;
    g.currencySymbol = curr.symbol;
  });

  if (returnToProfile) {
    openProfileModal();
  } else {
    closeModal();
    renderApp();
    showToast(`Currency updated to ${curr.flag} ${curr.code} (${curr.symbol})`);
  }
}

// ============================================================================
// 3. CREATE NEW GROUP MODAL
// ============================================================================
let newGroupIcon = 'flight_takeoff';

function openNewGroupModal() {
  haptic('tap');
  newGroupIcon = 'flight_takeoff';
  const root = document.getElementById('app-modal-root');
  if (!root) return;

  root.innerHTML = `
    <div class="fixed inset-0 z-50 bg-black/45 backdrop-blur-xs flex items-end justify-center" onclick="if(event.target===this) closeModal();">
      <div class="w-full max-w-md bg-[#FAF6F0] rounded-t-[32px] p-6 border-t border-[#EDE7DF] shadow-2xl space-y-4 animate-slide-up">
        <div class="flex items-center justify-between">
          <h2 class="text-[18px] font-extrabold text-[#23201E]">Create New Group</h2>
          <button onclick="closeModal()" class="w-8 h-8 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] font-bold">✕</button>
        </div>

        <div class="space-y-1.5">
          <label class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Group Name</label>
          <input id="new-group-name" type="text" placeholder="e.g. Manali Roadtrip, Flat 302, Team Lunch" class="w-full h-11 px-4 rounded-2xl bg-white border border-[#EDE7DF] text-[14px] font-extrabold text-[#23201E] focus:outline-none"/>
        </div>

        <div class="space-y-1.5">
          <label class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Category Icon</label>
          <div class="grid grid-cols-6 gap-2" id="new-group-icon-grid">
            ${GROUP_ICONS.map(ic => `
              <button onclick="newGroupIcon='${ic.icon}'; document.querySelectorAll('#new-group-icon-grid button').forEach(b=>b.classList.remove('bg-[#D7E8B6]','border-[#365314]')); this.classList.add('bg-[#D7E8B6]','border-[#365314]');" class="h-11 rounded-2xl border border-[#EDE7DF] bg-white flex items-center justify-center ${ic.icon === newGroupIcon ? 'bg-[#D7E8B6] border-[#365314]' : ''}">
                <span class="material-symbols-outlined text-[20px] text-[#23201E]">${ic.icon}</span>
              </button>
            `).join('')}
          </div>
        </div>

        <div class="space-y-1.5">
          <label class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Friends / Members (comma separated)</label>
          <input id="new-group-members" type="text" placeholder="e.g. Rohan, Sneha, Vikram" class="w-full h-11 px-4 rounded-2xl bg-white border border-[#EDE7DF] text-[13px] font-bold text-[#23201E] focus:outline-none"/>
          <p class="text-[11px] text-[#6E675F]">We automatically generate illustrated character portraits for each friend.</p>
        </div>

        <button onclick="createNewGroupAction()" class="w-full h-12 rounded-2xl bg-[#23201E] text-white text-[14px] font-extrabold shadow-md active:scale-95">
          Create Group →
        </button>
      </div>
    </div>
  `;
}

function createNewGroupAction() {
  const nameEl = document.getElementById('new-group-name');
  const membersEl = document.getElementById('new-group-members');
  const name = (nameEl && nameEl.value.trim()) || '';
  if (!name) {
    showToast('Please enter a group name', 'terracotta');
    return;
  }
  const rawNames = ((membersEl && membersEl.value) || 'Rohan, Priya')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);

  const members = [
    { id: 'm_me', name: STATE.user.name, avatar: STATE.user.avatar, isMe: true }
  ];
  rawNames.forEach((n, idx) => {
    members.push({
      id: 'm_' + Date.now() + '_' + idx,
      name: n,
      avatar: AVATAR_GALLERY[(idx + 1) % AVATAR_GALLERY.length].url,
      isMe: false
    });
  });

  const newGroup = {
    id: 'g_' + Date.now(),
    name,
    icon: newGroupIcon,
    currencyCode: STATE.user.currencyCode,
    currencySymbol: STATE.user.currencySymbol,
    createdAt: Date.now(),
    members
  };

  STATE.groups.unshift(newGroup);
  STATE.activeGroupId = newGroup.id;
  haptic('confirm');
  closeModal();
  renderApp();
  showToast(`Created group "${name}" with ${members.length} members`);
}

// ============================================================================
// 4. ADD MEMBER TO GROUP MODAL
// ============================================================================
let addMemberAvatarUrl = 'avatars/peep_4.svg';

function openAddMemberModal(groupId) {
  haptic('tap');
  addMemberAvatarUrl = AVATAR_GALLERY[Math.floor(Math.random() * AVATAR_GALLERY.length)].url;
  const group = STATE.groups.find(g => g.id === groupId);
  if (!group) return;

  const root = document.getElementById('app-modal-root');
  root.innerHTML = `
    <div class="fixed inset-0 z-50 bg-black/45 backdrop-blur-xs flex items-end justify-center" onclick="if(event.target===this) closeModal();">
      <div class="w-full max-w-md bg-[#FAF6F0] rounded-t-[32px] p-6 border-t border-[#EDE7DF] shadow-2xl space-y-4 animate-slide-up">
        <div class="flex items-center justify-between">
          <h2 class="text-[17px] font-extrabold text-[#23201E]">Add Friend to ${escapeHtml(group.name)}</h2>
          <button onclick="closeModal()" class="w-8 h-8 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] font-bold">✕</button>
        </div>

        <div class="bg-white rounded-[22px] p-3.5 border border-[#EDE7DF] flex items-center gap-3">
          <div id="new-member-avatar-box">${renderAvatarImg(addMemberAvatarUrl, 'Friend', 'w-12 h-12')}</div>
          <input id="new-member-name" type="text" placeholder="Friend's Name (e.g. Vikram)" class="flex-1 h-10 px-3 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[14px] font-extrabold text-[#23201E] focus:outline-none"/>
        </div>

        <div class="grid grid-cols-6 gap-2">
          ${AVATAR_GALLERY.map(av => `
            <button onclick="addMemberAvatarUrl='${av.url}'; document.getElementById('new-member-avatar-box').innerHTML=renderAvatarImg('${av.url}','Friend','w-12 h-12');" class="p-1 rounded-xl bg-white border border-[#EDE7DF] flex items-center justify-center">
              <img src="${av.url}" class="w-9 h-9 rounded-full"/>
            </button>
          `).join('')}
        </div>

        <button onclick="confirmAddMember('${group.id}')" class="w-full h-12 rounded-2xl bg-[#23201E] text-white text-[14px] font-extrabold">
          + Add to Group
        </button>
      </div>
    </div>
  `;
}

function confirmAddMember(groupId) {
  const group = STATE.groups.find(g => g.id === groupId);
  const input = document.getElementById('new-member-name');
  const name = (input && input.value.trim()) || '';
  if (!group || !name) {
    showToast('Enter your friend’s name', 'terracotta');
    return;
  }
  group.members.push({
    id: 'm_' + Date.now(),
    name,
    avatar: addMemberAvatarUrl,
    isMe: false
  });
  haptic('confirm');
  closeModal();
  renderApp();
  showToast(`Added ${name} to ${group.name}`);
}

// ============================================================================
// 5. QUICK EXPENSE LOGGER & CALCULATOR MODAL (0.00¢ Drift)
// ============================================================================
let qeState = {
  groupId: '',
  title: 'Dinner & Groceries',
  payerId: 'm_me',
  cents: 125000, // 1,250.00
  selectedMemberIds: []
};

function openQuickExpenseModal(defaultGroupId) {
  haptic('tap');
  const group = STATE.groups.find(g => g.id === (defaultGroupId || STATE.activeGroupId)) || STATE.groups[0];
  if (!group) return;

  qeState = {
    groupId: group.id,
    title: '',
    payerId: group.members[0].id,
    cents: 4250, // 42.50 default so user sees instant penny-perfect 14.17 / 14.17 / 14.16 split
    selectedMemberIds: group.members.map(m => m.id)
  };
  renderQuickExpenseModalDOM();
}

function renderQuickExpenseModalDOM() {
  const root = document.getElementById('app-modal-root');
  const group = STATE.groups.find(g => g.id === qeState.groupId) || STATE.groups[0];
  if (!root || !group) return;

  const splits = splitCentsZeroDrift(qeState.cents, qeState.selectedMemberIds);

  root.innerHTML = `
    <div class="fixed inset-0 z-50 bg-black/45 backdrop-blur-xs flex items-end justify-center" onclick="if(event.target===this) closeModal();">
      <div class="w-full max-w-md bg-[#FAF6F0] rounded-t-[32px] p-5 border-t border-[#EDE7DF] shadow-2xl space-y-3.5 max-h-[94vh] overflow-y-auto animate-slide-up">
        <!-- Top Header -->
        <div class="flex items-center justify-between">
          <select onchange="qeState.groupId=this.value; const g=STATE.groups.find(x=>x.id===this.value); qeState.payerId=g.members[0].id; qeState.selectedMemberIds=g.members.map(m=>m.id); renderQuickExpenseModalDOM();" class="h-9 px-3 rounded-full bg-white border border-[#EDE7DF] text-[12px] font-extrabold text-[#23201E]">
            ${STATE.groups.map(g => `<option value="${g.id}" ${g.id === group.id ? 'selected' : ''}>${escapeHtml(g.name)}</option>`).join('')}
          </select>
          <div class="flex items-center gap-1.5">
            <button onclick="qeState.cents=0; renderQuickExpenseModalDOM();" class="h-8 px-3 rounded-full bg-white border border-[#EDE7DF] text-[11px] font-bold text-[#6E675F]">Clear</button>
            <button onclick="closeModal()" class="w-8 h-8 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] font-bold">✕</button>
          </div>
        </div>

        <!-- Expense Description & Amount Display -->
        <div class="bg-white rounded-[26px] p-4 border border-[#EDE7DF] text-center space-y-2">
          <input id="qe-title-input" type="text" value="${escapeHtml(qeState.title)}" oninput="qeState.title=this.value" placeholder="What was this expense for? (e.g. Groceries)" class="w-full text-center text-[13px] font-extrabold text-[#23201E] bg-[#FAF6F0] h-9 px-3 rounded-xl border border-[#EDE7DF] focus:outline-none"/>
          <div class="text-[36px] font-extrabold text-[#23201E] tabular-nums leading-none pt-1">
            ${fmtMoney(qeState.cents, group.currencySymbol)}
          </div>
        </div>

        <!-- Paid By Selector -->
        <div class="space-y-1">
          <span class="text-[10px] font-extrabold uppercase tracking-wider text-[#6E675F] px-1">Paid By</span>
          <div class="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
            ${group.members.map(m => `
              <button onclick="qeState.payerId='${m.id}'; renderQuickExpenseModalDOM();" class="shrink-0 flex items-center gap-1.5 pl-1.5 pr-3 py-1 rounded-full border text-[12px] font-extrabold ${qeState.payerId === m.id ? 'bg-[#D7E8B6] border-[#365314] text-[#2B4810]' : 'bg-white border-[#EDE7DF] text-[#23201E]'}">
                ${renderAvatarImg(m.avatar, m.name, 'w-6 h-6')}
                <span>${escapeHtml(m.name)}</span>
              </button>
            `).join('')}
          </div>
        </div>

        <!-- Live Largest-Remainder Split Preview (Tap member to include/exclude) -->
        <div class="bg-white rounded-[22px] p-3.5 border border-[#EDE7DF] space-y-2">
          <div class="flex items-center justify-between text-[11px] font-extrabold">
            <span class="text-[#23201E]">Split Equally (${qeState.selectedMemberIds.length} selected)</span>
            <span class="text-[#365314]">⚖️ 0.00 Rounding Drift</span>
          </div>
          <div class="grid grid-cols-2 gap-1.5">
            ${group.members.map(m => {
              const sp = splits.find(s => s.memberId === m.id);
              const included = Boolean(sp);
              return `
                <button onclick="toggleQeMember('${m.id}')" class="p-2 rounded-xl border text-left flex items-center justify-between ${included ? 'bg-[#FAF6F0] border-[#23201E]' : 'bg-white/50 border-[#EDE7DF] opacity-50'}">
                  <div class="flex items-center gap-1.5 min-w-0">
                    ${renderAvatarImg(m.avatar, m.name, 'w-5 h-5')}
                    <span class="text-[11px] font-bold text-[#23201E] truncate">${escapeHtml(m.name)}${sp && sp.plusOneCent ? ' (+1¢)' : ''}</span>
                  </div>
                  <span class="text-[11px] font-extrabold tabular-nums text-[#23201E]">${sp ? fmtMoney(sp.cents, group.currencySymbol) : fmtMoney(0, group.currencySymbol)}</span>
                </button>
              `;
            }).join('')}
          </div>
        </div>

        <!-- Quick Add Chips -->
        <div class="flex items-center gap-1.5 justify-center">
          ${[
            { label: '+10', add: 1000 },
            { label: '+50', add: 5000 },
            { label: '+100', add: 10000 },
            { label: '+500', add: 50000 },
            { label: '+18% Tip', mult: 1.18 }
          ].map(ch => `
            <button onclick="qeKeyAction('${ch.label}', ${ch.add || 0}, ${ch.mult || 0})" class="h-8 px-3 rounded-full bg-white border border-[#EDE7DF] text-[11px] font-extrabold text-[#23201E] active:scale-95">
              ${ch.label}
            </button>
          `).join('')}
        </div>

        <!-- 3x4 Tactile Keypad -->
        <div class="grid grid-cols-3 gap-2">
          ${['1','2','3','4','5','6','7','8','9','00','0','⌫'].map(k => `
            <button onclick="qeDigitPress('${k}')" class="h-11 rounded-2xl bg-white border border-[#EDE7DF] text-[17px] font-extrabold text-[#23201E] shadow-2xs active:scale-95">
              ${k}
            </button>
          `).join('')}
        </div>

        <button onclick="saveQuickExpenseAction()" class="w-full h-12 rounded-2xl bg-[#365314] text-white text-[14px] font-extrabold shadow-md active:scale-95">
          ✓ Save Expense (${fmtMoney(qeState.cents, group.currencySymbol)})
        </button>
      </div>
    </div>
  `;
}

function toggleQeMember(memberId) {
  haptic('tap');
  const idx = qeState.selectedMemberIds.indexOf(memberId);
  if (idx >= 0) {
    if (qeState.selectedMemberIds.length > 1) qeState.selectedMemberIds.splice(idx, 1);
  } else {
    qeState.selectedMemberIds.push(memberId);
  }
  renderQuickExpenseModalDOM();
}

function qeKeyAction(label, addCents, mult) {
  haptic('clock_tick');
  if (addCents) qeState.cents += addCents;
  if (mult) qeState.cents = Math.round(qeState.cents * mult);
  renderQuickExpenseModalDOM();
}

function qeDigitPress(k) {
  haptic('clock_tick');
  if (k === '⌫') {
    qeState.cents = Math.floor(qeState.cents / 10);
  } else if (k === '00') {
    if (qeState.cents < 9999999) qeState.cents = qeState.cents * 100;
  } else {
    const d = parseInt(k, 10);
    if (!isNaN(d) && qeState.cents < 99999999) {
      qeState.cents = qeState.cents * 10 + d;
    }
  }
  renderQuickExpenseModalDOM();
}

function saveQuickExpenseAction() {
  if (qeState.cents <= 0) {
    showToast('Enter an amount greater than 0', 'terracotta');
    return;
  }
  const group = STATE.groups.find(g => g.id === qeState.groupId) || STATE.groups[0];
  const title = (qeState.title && qeState.title.trim()) || 'Group Expense';
  const splits = splitCentsZeroDrift(qeState.cents, qeState.selectedMemberIds);

  STATE.expenses.unshift({
    id: 'exp_' + Date.now(),
    groupId: group.id,
    title,
    category: 'General',
    payerId: qeState.payerId,
    totalCents: qeState.cents,
    createdAt: Date.now(),
    type: 'expense',
    splits
  });

  try {
    if (window.SplitMateNative && window.SplitMateNative.logQuickExpense) {
      window.SplitMateNative.logQuickExpense(title, 'General', group.id, qeState.payerId, qeState.cents);
    }
  } catch (e) {}

  haptic('confirm');
  closeModal();
  renderApp();
  showToast(`Saved "${title}" (${fmtMoney(qeState.cents, group.currencySymbol)})`);
}
