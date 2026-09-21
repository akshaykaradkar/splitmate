/**
 * SplitMate v2.0 — Dynamic UI Controller, Modals, and 4-Tab Reactive Renderer
 */

// --- Avatar Helper with Fallback ---
function renderAvatarImg(url, name, sizeClass = 'w-10 h-10') {
  const fallback = getFallbackLocalAvatar(name);
  const src = url || fallback;
  return `<img src="${src}" onerror="this.onerror=null;this.src='${fallback}';" alt="${escapeHtml(name)}" class="${sizeClass} rounded-full object-cover bg-[#F4EFE6] border border-[#EDE7DF] shrink-0"/>`;
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function timeAgo(ts) {
  const diffSec = Math.max(1, Math.floor((Date.now() - ts) / 1000));
  if (diffSec < 60) return 'Just now';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return `${Math.floor(diffSec / 86400)}d ago`;
}

// --- Main Application Render Dispatcher ---
function renderApp() {
  saveState();
  renderHeader();
  renderBottomNav();

  const container = document.getElementById('app-main');
  if (!container) return;

  if (STATE.activeTab === 'groups') {
    container.innerHTML = renderGroupsTab();
  } else if (STATE.activeTab === 'split') {
    container.innerHTML = renderSplitReceiptTab();
  } else if (STATE.activeTab === 'settle') {
    container.innerHTML = renderSettleTab();
  } else if (STATE.activeTab === 'activity') {
    container.innerHTML = renderActivityTab();
  }
}

function switchTab(tab) {
  haptic('tap');
  STATE.activeTab = tab;
  renderApp();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// --- Header Renderer ---
function renderHeader() {
  const hdr = document.getElementById('app-header');
  if (!hdr) return;

  const subtitles = {
    groups: 'Groups & Balances',
    split: 'Itemized Bill Splitter',
    settle: 'Simplified Settle Up',
    activity: 'Activity & On-Device Vault'
  };

  hdr.innerHTML = `
    <div class="h-16 px-5 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <img src="app_icon.svg" alt="SplitMate" class="w-10 h-10 rounded-2xl shadow-sm shrink-0"/>
        <div class="flex flex-col">
          <span class="text-[19px] font-extrabold tracking-tight text-[#23201E] leading-none">SplitMate</span>
          <span class="text-[11px] font-semibold text-[#6E675F] mt-0.5">${subtitles[STATE.activeTab] || 'Groups & Balances'}</span>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <!-- Active Currency Chip (161 World Currencies) -->
        <button onclick="openCurrencyModal()" class="h-9 px-3 rounded-full bg-white border border-[#EDE7DF] shadow-2xs flex items-center gap-1.5 active:scale-95 transition-all">
          <span class="text-[14px]">${STATE.user.currencyFlag || '🇮🇳'}</span>
          <span class="text-[12px] font-extrabold text-[#23201E]">${escapeHtml(STATE.user.currencyCode)} (${escapeHtml(STATE.user.currencySymbol)})</span>
          <span class="material-symbols-outlined text-[15px] text-[#6E675F]">expand_more</span>
        </button>

        <!-- User Character Profile Button -->
        <button onclick="openProfileModal()" class="flex items-center gap-1.5 pl-1 pr-2.5 py-1 rounded-full bg-white border border-[#EDE7DF] shadow-2xs active:scale-95 transition-all" title="Edit Profile & Avatar">
          ${renderAvatarImg(STATE.user.avatar, STATE.user.name, 'w-7 h-7')}
          <span class="text-[12px] font-bold text-[#23201E] max-w-[72px] truncate">${escapeHtml(STATE.user.name)}</span>
        </button>
      </div>
    </div>
  `;
}

// --- Bottom Navigation Renderer ---
function renderBottomNav() {
  const nav = document.getElementById('app-bottom-nav');
  if (!nav) return;

  // Count pending settlements across all groups
  let pendingCount = 0;
  STATE.groups.forEach(g => {
    const { transfers } = simplifyGroupDebtsGreedy(g.id);
    pendingCount += transfers.length;
  });

  const tabs = [
    { id: 'groups', icon: 'receipt_long', label: 'Groups' },
    { id: 'split', icon: 'document_scanner', label: 'Split Bill' },
    { id: 'settle', icon: 'account_balance', label: 'Settle Up', badge: pendingCount },
    { id: 'activity', icon: 'history', label: 'Activity' }
  ];

  nav.innerHTML = `
    <div class="h-16 px-3 flex items-center justify-around max-w-md mx-auto">
      ${tabs.map(t => {
        const active = STATE.activeTab === t.id;
        return `
          <button onclick="switchTab('${t.id}')" class="flex flex-col items-center justify-center min-w-[68px] py-1 active:scale-95 transition-all relative ${active ? 'text-[#23201E]' : 'text-[#8C847B]'}">
            <div class="flex items-center justify-center w-12 h-7 rounded-full transition-all relative ${active ? 'bg-[#D7E8B6] text-[#365314] shadow-2xs' : ''}">
              <span class="material-symbols-outlined text-[20px]">${t.icon}</span>
              ${t.badge ? `<span class="absolute -top-1 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-[#E06B52] text-white text-[10px] font-extrabold flex items-center justify-center">${t.badge}</span>` : ''}
            </div>
            <span class="text-[11px] ${active ? 'font-extrabold' : 'font-semibold'} mt-1 tracking-tight">${t.label}</span>
          </button>
        `;
      }).join('')}
    </div>
  `;
}

// ============================================================================
// TAB 1: GROUPS & LEDGERS HUB (100% Dynamic)
// ============================================================================
function renderGroupsTab() {
  let overallNetCents = 0;
  const groupCardsData = STATE.groups.map(g => {
    const { balances, transfers } = simplifyGroupDebtsGreedy(g.id);
    const myEntry = balances.find(b => b.member.isMe || b.member.id === 'm_me');
    const myNet = myEntry ? myEntry.netCents : 0;
    overallNetCents += myNet;
    return { group: g, balances, transfers, myNet };
  });

  const filteredGroups = groupCardsData.filter(item => {
    if (STATE.groupFilter === 'owed') return item.myNet > 1;
    if (STATE.groupFilter === 'owe') return item.myNet < -1;
    if (STATE.groupFilter === 'settled') return Math.abs(item.myNet) <= 1;
    return true;
  });

  // Hero theme based on net balance
  let heroBg = 'from-[#D7E8B6] via-[#E6F2CE] to-[#F3F8E6] border-[#C7DF9E]';
  let heroPillText = 'You are owed overall';
  let heroPillColor = 'text-[#365314]';
  let heroAmountColor = 'text-[#23201E]';
  let heroSub = `Across ${STATE.groups.length} active group${STATE.groups.length === 1 ? '' : 's'}`;

  if (overallNetCents < -1) {
    heroBg = 'from-[#FED8C8] via-[#FCE3D7] to-[#FDEBE2] border-[#F7C2AD]';
    heroPillText = 'You owe overall';
    heroPillColor = 'text-[#7C2D12]';
    heroAmountColor = 'text-[#9A2B15]';
  } else if (Math.abs(overallNetCents) <= 1) {
    heroBg = 'from-[#E8F2D6] via-[#F4EFE6] to-[#FCE5D8] border-[#E5DEC9]';
    heroPillText = 'All settled up';
    heroPillColor = 'text-[#365314]';
    heroSub = 'Every group balance is in equilibrium';
  }

  return `
    <div class="space-y-5 pb-8">
      <!-- Hero Net Balance Bento Card -->
      <section class="bg-white rounded-[30px] p-5 border border-[#EDE7DF] shadow-[0_6px_24px_rgba(35,32,30,0.04)] space-y-4">
        <div class="rounded-[24px] bg-gradient-to-br ${heroBg} border p-5 relative overflow-hidden">
          <div class="flex items-center justify-between">
            <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/90 ${heroPillColor} text-[11px] font-extrabold shadow-2xs">
              <span class="material-symbols-outlined text-[14px]">${overallNetCents < -1 ? 'call_made' : (overallNetCents > 1 ? 'call_received' : 'check_circle')}</span>
              ${heroPillText}
            </span>
            <button onclick="switchTab('settle')" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#23201E]/10 text-[#23201E] text-[11px] font-bold">
              <span>View Settle Plan</span>
              <span class="material-symbols-outlined text-[13px]">arrow_forward</span>
            </button>
          </div>

          <div class="pt-4 pb-1 flex items-baseline gap-2">
            <span class="text-[36px] font-extrabold tracking-tight ${heroAmountColor} tabular-nums leading-none">
              ${overallNetCents > 1 ? '+' : ''}${fmtMoney(overallNetCents)}
            </span>
          </div>

          <p class="text-[12px] font-semibold text-[#5C554E] mt-1">${heroSub}</p>
        </div>

        <!-- Primary Action Row -->
        <div class="grid grid-cols-2 gap-2.5">
          <button onclick="openQuickExpenseModal()" class="h-12 rounded-2xl bg-[#23201E] text-[#FAF6F0] text-[13px] font-extrabold flex items-center justify-center gap-2 shadow-sm active:scale-95 transition-all">
            <span class="material-symbols-outlined text-[18px]">add_circle</span>
            <span>+ Add Expense</span>
          </button>
          <button onclick="openNewGroupModal()" class="h-12 rounded-2xl bg-[#D7E8B6] text-[#2B4810] text-[13px] font-extrabold flex items-center justify-center gap-2 active:scale-95 transition-all">
            <span class="material-symbols-outlined text-[18px]">group_add</span>
            <span>+ New Group</span>
          </button>
        </div>
      </section>

      <!-- Filter Bar -->
      <div class="flex items-center justify-between gap-2 overflow-x-auto pb-1 no-scrollbar">
        <div class="flex items-center gap-1.5">
          ${[
            { id: 'all', label: `All (${STATE.groups.length})` },
            { id: 'owed', label: 'Owed to You' },
            { id: 'owe', label: 'You Owe' },
            { id: 'settled', label: 'Settled' }
          ].map(f => `
            <button onclick="STATE.groupFilter='${f.id}';renderApp();" class="h-8 px-3.5 rounded-full text-[12px] font-bold transition-all ${STATE.groupFilter === f.id ? 'bg-[#23201E] text-[#FAF6F0]' : 'bg-white text-[#6E675F] border border-[#EDE7DF]'}">
              ${f.label}
            </button>
          `).join('')}
        </div>
      </div>

      <!-- Dynamic Group Cards List -->
      <section class="space-y-3.5">
        ${filteredGroups.length === 0 ? `
          <div class="bg-white rounded-[28px] p-8 border border-dashed border-[#D8D0C5] text-center space-y-3">
            <div class="w-14 h-14 rounded-2xl bg-[#D7E8B6]/60 text-[#365314] mx-auto flex items-center justify-center">
              <span class="material-symbols-outlined text-[28px]">group_add</span>
            </div>
            <div class="space-y-1">
              <h3 class="text-[16px] font-extrabold text-[#23201E]">No groups in this filter</h3>
              <p class="text-[12px] text-[#6E675F]">Create a new group for a trip, apartment, or dinner to start splitting bills.</p>
            </div>
            <button onclick="openNewGroupModal()" class="h-10 px-4 rounded-full bg-[#23201E] text-white text-[12px] font-bold inline-flex items-center gap-1.5">
              <span class="material-symbols-outlined text-[16px]">add</span>
              <span>Create Group</span>
            </button>
          </div>
        ` : filteredGroups.map(({ group, balances, transfers, myNet }) => {
          const isExpanded = STATE.activeGroupId === group.id;
          const groupExpenses = STATE.expenses.filter(e => e.groupId === group.id);
          const cardBg = myNet > 1 ? 'bg-[#F3F8E9] border-[#D7E8B6]' : (myNet < -1 ? 'bg-[#FDF0EB] border-[#F8D0C0]' : 'bg-white border-[#EDE7DF]');
          const netColor = myNet > 1 ? 'text-[#365314]' : (myNet < -1 ? 'text-[#B91C1C]' : 'text-[#6E675F]');
          const netLabel = myNet > 1 ? 'You get back' : (myNet < -1 ? 'You owe' : 'Settled');

          return `
            <article class="rounded-[28px] p-5 border ${cardBg} shadow-2xs transition-all space-y-3.5">
              <div class="flex items-start justify-between cursor-pointer" onclick="STATE.activeGroupId = (STATE.activeGroupId === '${group.id}' ? '' : '${group.id}'); renderApp();">
                <div class="flex items-start gap-3 min-w-0">
                  <div class="w-11 h-11 rounded-2xl bg-white flex items-center justify-center shrink-0 shadow-2xs border border-[#EDE7DF]">
                    <span class="material-symbols-outlined text-[#23201E] text-[22px]">${group.icon || 'groups'}</span>
                  </div>
                  <div class="min-w-0">
                    <div class="flex items-center gap-1.5">
                      <h3 class="text-[16px] font-extrabold text-[#23201E] truncate">${escapeHtml(group.name)}</h3>
                    </div>
                    <p class="text-[12px] text-[#6E675F] font-medium mt-0.5">${group.members.length} members · ${groupExpenses.length} expense${groupExpenses.length === 1 ? '' : 's'}</p>
                  </div>
                </div>

                <div class="flex flex-col items-end shrink-0 pl-2">
                  <span class="text-[10px] uppercase tracking-wider font-extrabold ${netColor}">${netLabel}</span>
                  <span class="text-[19px] font-extrabold tabular-nums ${netColor}">
                    ${myNet > 1 ? '+' : ''}${fmtMoney(myNet, group.currencySymbol)}
                  </span>
                </div>
              </div>

              <!-- Illustrated Member Character Portraits Row + Add Member Button -->
              <div class="flex items-center justify-between pt-1">
                <div class="flex items-center -space-x-2 overflow-hidden py-0.5">
                  ${group.members.map(m => `
                    <div class="relative inline-block rounded-full ring-2 ring-white" title="${escapeHtml(m.name)}">
                      ${renderAvatarImg(m.avatar, m.name, 'w-8 h-8')}
                    </div>
                  `).join('')}
                  <button onclick="openAddMemberModal('${group.id}')" class="w-8 h-8 rounded-full bg-white border border-dashed border-[#99928A] text-[#23201E] text-[11px] font-extrabold flex items-center justify-center ring-2 ring-white hover:bg-[#FAF6F0]" title="Add Friend to Group">+</button>
                </div>

                <div class="flex items-center gap-1.5">
                  <button onclick="openQuickExpenseModal('${group.id}')" class="h-8 px-3 rounded-full bg-white border border-[#EDE7DF] text-[#23201E] text-[11px] font-extrabold flex items-center gap-1 shadow-2xs">
                    <span class="material-symbols-outlined text-[14px]">add</span>
                    <span>Expense</span>
                  </button>
                  <button onclick="STATE.activeGroupId='${group.id}'; switchTab('settle');" class="h-8 px-3 rounded-full bg-[#23201E] text-white text-[11px] font-extrabold flex items-center gap-1 shadow-2xs">
                    <span>Settle (${transfers.length})</span>
                  </button>
                </div>
              </div>

              ${isExpanded ? `
                <!-- Expanded Group Details & Expenses -->
                <div class="pt-3 border-t border-[#EDE7DF]/80 space-y-2.5">
                  <div class="flex items-center justify-between text-[11px] font-bold text-[#6E675F]">
                    <span>Group Member Balances</span>
                    <button onclick="STATE.receiptDraft.groupId='${group.id}'; switchTab('split');" class="text-[#365314] underline font-extrabold">Split Itemized Receipt →</button>
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    ${balances.map(b => `
                      <div class="bg-white/90 rounded-2xl p-2.5 border border-[#EDE7DF] flex items-center justify-between">
                        <div class="flex items-center gap-2 min-w-0">
                          ${renderAvatarImg(b.member.avatar, b.member.name, 'w-6 h-6')}
                          <span class="text-[12px] font-bold text-[#23201E] truncate">${escapeHtml(b.member.name)}</span>
                        </div>
                        <span class="text-[12px] font-extrabold tabular-nums ${b.netCents > 1 ? 'text-[#365314]' : (b.netCents < -1 ? 'text-[#B91C1C]' : 'text-[#6E675F]')}">
                          ${b.netCents > 1 ? '+' : ''}${fmtMoney(b.netCents, group.currencySymbol)}
                        </span>
                      </div>
                    `).join('')}
                  </div>
                </div>
              ` : ''}
            </article>
          `;
        }).join('')}
      </section>
    </div>
  `;
}
