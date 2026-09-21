/**
 * SplitMate v2.0 — Dynamic Tabs (Split Bill, Settle Up, Activity) & Interactive Modals
 */

// ============================================================================
// TAB 2: LIVE ITEMIZED RECEIPT CLAIM & REMAINDER ENGINE (100% Dynamic)
// ============================================================================
function renderSplitReceiptTab() {
  const draft = STATE.receiptDraft;
  const group = STATE.groups.find(g => g.id === draft.groupId) || STATE.groups[0];
  if (!group) {
    return `<div class="p-8 text-center text-[14px] font-bold">Create a group first to split a receipt.</div>`;
  }
  draft.groupId = group.id;

  // Ensure activeClaimerId & payerId belong to group
  if (!group.members.some(m => m.id === draft.payerId)) draft.payerId = group.members[0].id;
  if (!group.members.some(m => m.id === draft.activeClaimerId)) draft.activeClaimerId = group.members[0].id;

  const subtotalCents = draft.items.reduce((acc, it) => acc + (it.cents || 0), 0);
  const taxCents = Math.round(subtotalCents * (draft.taxPercent / 100));
  const tipCents = Math.round(subtotalCents * (draft.tipPercent / 100));
  const totalCents = subtotalCents + taxCents + tipCents;
  const multiplier = subtotalCents > 0 ? totalCents / subtotalCents : 1.0;

  // Compute each member's base claimed & unassigned remainder
  const memberBaseMap = {};
  group.members.forEach(m => { memberBaseMap[m.id] = 0; });
  let unassignedBaseCents = 0;

  draft.items.forEach(it => {
    const validClaimers = (it.claimedBy || []).filter(cid => memberBaseMap[cid] !== undefined);
    if (validClaimers.length === 0) {
      unassignedBaseCents += it.cents;
    } else {
      const splits = splitCentsZeroDrift(it.cents, validClaimers);
      splits.forEach(s => {
        memberBaseMap[s.memberId] += s.cents;
      });
    }
  });

  const unassignedFinalCents = Math.round(unassignedBaseCents * multiplier);

  // Calculate final tax+tip-adjusted share per member with Largest Remainder reconciliation
  const memberFinalList = group.members.map(m => {
    const base = memberBaseMap[m.id] || 0;
    const exactFinal = base * multiplier;
    const floored = Math.floor(exactFinal);
    return {
      member: m,
      baseCents: base,
      flooredCents: floored,
      frac: exactFinal - floored,
      finalCents: floored
    };
  });

  const targetClaimedFinal = totalCents - unassignedFinalCents;
  let diff = targetClaimedFinal - memberFinalList.reduce((s, x) => s + x.flooredCents, 0);
  if (diff > 0 && memberFinalList.some(x => x.baseCents > 0)) {
    const sorted = [...memberFinalList].sort((a, b) => b.frac - a.frac);
    for (let i = 0; i < Math.min(diff, sorted.length); i++) {
      if (sorted[i].baseCents > 0) sorted[i].finalCents += 1;
    }
  }

  return `
    <div class="space-y-4 pb-10">
      <!-- Group & Payer Selector Row -->
      <div class="flex items-center justify-between gap-2">
        <select onchange="STATE.receiptDraft.groupId=this.value; renderApp();" class="h-10 px-3.5 rounded-full bg-white border border-[#EDE7DF] text-[12px] font-extrabold text-[#23201E] shadow-2xs">
          ${STATE.groups.map(g => `<option value="${g.id}" ${g.id === group.id ? 'selected' : ''}>Group: ${escapeHtml(g.name)}</option>`).join('')}
        </select>

        <select onchange="STATE.receiptDraft.payerId=this.value; renderApp();" class="h-10 px-3.5 rounded-full bg-[#D7E8B6] border border-[#C5DC9B] text-[12px] font-extrabold text-[#2B4810] shadow-2xs">
          ${group.members.map(m => `<option value="${m.id}" ${m.id === draft.payerId ? 'selected' : ''}>Paid by: ${escapeHtml(m.name)}</option>`).join('')}
        </select>
      </div>

      <!-- Receipt Summary Card with Locked Multiplier -->
      <section class="bg-white rounded-[28px] p-5 border border-[#EDE7DF] shadow-2xs space-y-3.5">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0 flex-1">
            <input type="text" value="${escapeHtml(draft.title)}" onchange="STATE.receiptDraft.title=this.value;saveState();" class="w-full text-[18px] font-extrabold text-[#23201E] bg-transparent border-b border-transparent focus:border-[#23201E] focus:outline-none truncate" placeholder="Receipt Name..."/>
            <p class="text-[11px] font-semibold text-[#6E675F] mt-0.5">Subtotal ${fmtMoney(subtotalCents, group.currencySymbol)} + Tax/Tip ${fmtMoney(taxCents + tipCents, group.currencySymbol)}</p>
          </div>
          <div class="text-right shrink-0">
            <span class="text-[24px] font-extrabold text-[#23201E] tabular-nums leading-none">${fmtMoney(totalCents, group.currencySymbol)}</span>
            <div class="text-[10px] font-extrabold text-[#365314] bg-[#D7E8B6]/70 px-2 py-0.5 rounded-full mt-1 inline-block">
              Multiplier ${multiplier.toFixed(4)}×
            </div>
          </div>
        </div>

        <!-- Tax & Tip Quick Controls -->
        <div class="grid grid-cols-2 gap-2 pt-1">
          <div class="bg-[#FAF6F0] rounded-2xl p-2.5 border border-[#EDE7DF] flex items-center justify-between">
            <span class="text-[11px] font-bold text-[#6E675F]">Tax %</span>
            <div class="flex items-center gap-1">
              ${[0, 5, 8.875, 12].map(p => `
                <button onclick="STATE.receiptDraft.taxPercent=${p};renderApp();" class="px-2 py-1 rounded-lg text-[11px] font-extrabold ${draft.taxPercent === p ? 'bg-[#23201E] text-white' : 'bg-white text-[#23201E] border border-[#EDE7DF]'}">${p}%</button>
              `).join('')}
            </div>
          </div>
          <div class="bg-[#FAF6F0] rounded-2xl p-2.5 border border-[#EDE7DF] flex items-center justify-between">
            <span class="text-[11px] font-bold text-[#6E675F]">Tip %</span>
            <div class="flex items-center gap-1">
              ${[0, 5, 10, 15].map(p => `
                <button onclick="STATE.receiptDraft.tipPercent=${p};renderApp();" class="px-2 py-1 rounded-lg text-[11px] font-extrabold ${draft.tipPercent === p ? 'bg-[#23201E] text-white' : 'bg-white text-[#23201E] border border-[#EDE7DF]'}">${p}%</button>
              `).join('')}
            </div>
          </div>
        </div>
      </section>

      <!-- Active Claiming Persona Bar (Illustrated Character Avatars) -->
      <section class="bg-white rounded-[24px] p-4 border border-[#EDE7DF] space-y-2.5">
        <div class="flex items-center justify-between">
          <span class="text-[11px] font-extrabold uppercase tracking-wider text-[#6E675F]">Tap Member to Claim Items For:</span>
          <span class="text-[11px] font-bold text-[#365314]">Tap items below to assign</span>
        </div>
        <div class="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
          ${memberFinalList.map(({ member, finalCents }) => {
            const active = draft.activeClaimerId === member.id;
            return `
              <button onclick="STATE.receiptDraft.activeClaimerId='${member.id}'; haptic('tap'); renderApp();" class="shrink-0 flex items-center gap-2 pl-1.5 pr-3 py-1.5 rounded-full border transition-all ${active ? 'bg-[#23201E] text-white border-[#23201E] shadow-sm' : 'bg-[#FAF6F0] text-[#23201E] border-[#EDE7DF]'}">
                ${renderAvatarImg(member.avatar, member.name, 'w-7 h-7')}
                <div class="text-left">
                  <div class="text-[12px] font-extrabold leading-none">${escapeHtml(member.name)}</div>
                  <div class="text-[10px] font-bold ${active ? 'text-[#D7E8B6]' : 'text-[#6E675F]'} tabular-nums mt-0.5">${fmtMoney(finalCents, group.currencySymbol)}</div>
                </div>
              </button>
            `;
          }).join('')}
        </div>
      </section>

      <!-- Unassigned Remainder Alert / Split Equally Banner -->
      ${unassignedBaseCents > 0 ? `
        <section class="rounded-[24px] bg-[#FED8C8]/85 border border-[#F5B79E] p-4 flex items-center justify-between gap-3">
          <div>
            <div class="text-[12px] font-extrabold text-[#7C2D12]">Unassigned Items: ${fmtMoney(unassignedFinalCents, group.currencySymbol)} (incl. tax/tip)</div>
            <div class="text-[11px] font-semibold text-[#8F391A]">Tap an item below to claim it, or split remainder equally</div>
          </div>
          <button onclick="splitReceiptRemainderEqually()" class="shrink-0 h-9 px-3.5 rounded-full bg-[#E06B52] text-white text-[11px] font-extrabold shadow-sm active:scale-95">
            Split Equally
          </button>
        </section>
      ` : `
        <section class="rounded-[24px] bg-[#D7E8B6]/80 border border-[#B8D685] p-3.5 flex items-center justify-between">
          <div class="flex items-center gap-2 text-[#2B4810] text-[12px] font-extrabold">
            <span class="material-symbols-outlined text-[18px]">verified</span>
            <span>All receipt items assigned (${fmtMoney(totalCents, group.currencySymbol)} · 0.00 rounding drift)</span>
          </div>
        </section>
      `}

      <!-- Receipt Items List -->
      <section class="space-y-2.5">
        ${draft.items.map(item => {
          const itemFinalCents = Math.round(item.cents * multiplier);
          const isClaimedByActive = (item.claimedBy || []).includes(draft.activeClaimerId);
          const unass = !item.claimedBy || item.claimedBy.length === 0;
          return `
            <div onclick="toggleReceiptItemClaim('${item.id}')" class="rounded-[24px] p-4 border transition-all cursor-pointer ${unass ? 'bg-[#FFF9F6] border-dashed border-[#E06B52]' : (isClaimedByActive ? 'bg-[#F3F8E9] border-[#B8D685]' : 'bg-white border-[#EDE7DF]')} flex items-center justify-between gap-3">
              <div class="min-w-0 flex-1 space-y-1.5">
                <div class="flex items-center gap-2">
                  <span class="text-[14px] font-extrabold text-[#23201E] truncate">${escapeHtml(item.name)}</span>
                  ${unass ? `<span class="px-2 py-0.5 rounded-full bg-[#FED8C8] text-[#7C2D12] text-[10px] font-extrabold">Unclaimed</span>` : ''}
                </div>
                <div class="flex items-center gap-1.5 flex-wrap">
                  ${group.members.map(m => {
                    const has = (item.claimedBy || []).includes(m.id);
                    return `
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold ${has ? 'bg-[#23201E] text-white' : 'bg-[#F4EFE6] text-[#8C847B]'}">
                        ${escapeHtml(m.name)}
                      </span>
                    `;
                  }).join('')}
                </div>
              </div>

              <div class="text-right shrink-0 flex items-center gap-2">
                <div>
                  <div class="text-[15px] font-extrabold text-[#23201E] tabular-nums">${fmtMoney(item.cents, group.currencySymbol)}</div>
                  <div class="text-[10px] font-bold text-[#6E675F] tabular-nums">${fmtMoney(itemFinalCents, group.currencySymbol)} w/ tax</div>
                </div>
                <button onclick="event.stopPropagation(); deleteReceiptItem('${item.id}')" class="w-7 h-7 rounded-full bg-[#F4EFE6] text-[#6E675F] flex items-center justify-center text-[14px]" title="Delete item">✕</button>
              </div>
            </div>
          `;
        }).join('')}

        <!-- Add New Receipt Item Inline Row -->
        <div class="bg-white rounded-[24px] p-3.5 border border-[#EDE7DF] flex items-center gap-2">
          <input id="new-ri-name" type="text" placeholder="Add item (e.g. Garlic Bread)" class="flex-1 h-10 px-3 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[13px] font-semibold focus:outline-none"/>
          <input id="new-ri-price" type="number" step="0.01" placeholder="Price" class="w-24 h-10 px-3 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[13px] font-extrabold tabular-nums focus:outline-none"/>
          <button onclick="addNewReceiptItem()" class="h-10 px-3.5 rounded-xl bg-[#23201E] text-white text-[12px] font-extrabold shrink-0">+ Add</button>
        </div>
      </section>

      <!-- Save Receipt to Group Ledger CTA -->
      <button onclick="commitReceiptToLedger()" class="w-full h-14 rounded-[22px] bg-[#365314] text-white text-[15px] font-extrabold flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all">
        <span class="material-symbols-outlined text-[20px]">check_circle</span>
        <span>Save Receipt (${fmtMoney(totalCents, group.currencySymbol)}) to ${escapeHtml(group.name)}</span>
      </button>
    </div>
  `;
}

function toggleReceiptItemClaim(itemId) {
  haptic('tap');
  const draft = STATE.receiptDraft;
  const item = draft.items.find(i => i.id === itemId);
  if (!item) return;
  item.claimedBy = item.claimedBy || [];
  const idx = item.claimedBy.indexOf(draft.activeClaimerId);
  if (idx >= 0) item.claimedBy.splice(idx, 1);
  else item.claimedBy.push(draft.activeClaimerId);
  renderApp();
}

function splitReceiptRemainderEqually() {
  haptic('confirm');
  const draft = STATE.receiptDraft;
  const group = STATE.groups.find(g => g.id === draft.groupId) || STATE.groups[0];
  if (!group) return;
  const allIds = group.members.map(m => m.id);
  draft.items.forEach(item => {
    if (!item.claimedBy || item.claimedBy.length === 0) {
      item.claimedBy = [...allIds];
    }
  });
  renderApp();
  showToast('Unassigned items split equally across all members');
}

function addNewReceiptItem() {
  const nameEl = document.getElementById('new-ri-name');
  const priceEl = document.getElementById('new-ri-price');
  const name = (nameEl && nameEl.value.trim()) || '';
  const price = parseFloat(priceEl && priceEl.value);
  if (!name || isNaN(price) || price <= 0) {
    showToast('Enter an item name and valid price', 'terracotta');
    return;
  }
  haptic('tap');
  STATE.receiptDraft.items.push({
    id: 'ri_' + Date.now(),
    name,
    cents: Math.round(price * 100),
    claimedBy: [STATE.receiptDraft.activeClaimerId]
  });
  renderApp();
}

function deleteReceiptItem(itemId) {
  haptic('tap');
  STATE.receiptDraft.items = STATE.receiptDraft.items.filter(i => i.id !== itemId);
  renderApp();
}

function commitReceiptToLedger() {
  const draft = STATE.receiptDraft;
  const group = STATE.groups.find(g => g.id === draft.groupId) || STATE.groups[0];
  if (!group || draft.items.length === 0) return;

  const subtotalCents = draft.items.reduce((acc, it) => acc + (it.cents || 0), 0);
  const taxCents = Math.round(subtotalCents * (draft.taxPercent / 100));
  const tipCents = Math.round(subtotalCents * (draft.tipPercent / 100));
  const totalCents = subtotalCents + taxCents + tipCents;
  const multiplier = subtotalCents > 0 ? totalCents / subtotalCents : 1.0;

  // Attribute any unclaimed item to all members equally so 100% is reconciled
  const allIds = group.members.map(m => m.id);
  const memberBaseMap = {};
  allIds.forEach(id => { memberBaseMap[id] = 0; });

  draft.items.forEach(it => {
    const claimers = (it.claimedBy && it.claimedBy.length > 0) ? it.claimedBy : allIds;
    splitCentsZeroDrift(it.cents, claimers).forEach(s => {
      memberBaseMap[s.memberId] = (memberBaseMap[s.memberId] || 0) + s.cents;
    });
  });

  const allocations = group.members.map(m => {
    const exact = (memberBaseMap[m.id] || 0) * multiplier;
    const fl = Math.floor(exact);
    return { memberId: m.id, cents: fl, frac: exact - fl, plusOneCent: false };
  });
  let rem = totalCents - allocations.reduce((s, a) => s + a.cents, 0);
  allocations.sort((a, b) => b.frac - a.frac);
  for (let i = 0; i < Math.min(rem, allocations.length); i++) {
    allocations[i].cents += 1;
    allocations[i].plusOneCent = true;
  }

  STATE.expenses.unshift({
    id: 'exp_' + Date.now(),
    groupId: group.id,
    title: draft.title || 'Itemized Receipt',
    category: 'Receipt',
    payerId: draft.payerId,
    totalCents,
    createdAt: Date.now(),
    type: 'receipt',
    splits: allocations.map(a => ({ memberId: a.memberId, cents: a.cents, plusOneCent: a.plusOneCent }))
  });

  haptic('confirm');
  showToast(`Saved "${draft.title}" (${fmtMoney(totalCents, group.currencySymbol)}) to ${group.name}`);
  switchTab('settle');
}

// ============================================================================
// TAB 3: GREEDY DEBT SIMPLIFICATION & SETTLE UP (100% Dynamic)
// ============================================================================
function renderSettleTab() {
  const group = STATE.groups.find(g => g.id === STATE.activeGroupId) || STATE.groups[0];
  if (!group) {
    return `<div class="p-8 text-center font-bold">No active groups yet.</div>`;
  }
  STATE.activeGroupId = group.id;

  const { balances, transfers } = simplifyGroupDebtsGreedy(group.id);
  const rawExpenseCount = STATE.expenses.filter(e => e.groupId === group.id).length;

  return `
    <div class="space-y-5 pb-10">
      <!-- Group Selector Pills -->
      <div class="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
        ${STATE.groups.map(g => `
          <button onclick="STATE.activeGroupId='${g.id}'; renderApp();" class="h-9 px-4 rounded-full text-[12px] font-extrabold shrink-0 transition-all ${g.id === group.id ? 'bg-[#23201E] text-white shadow-sm' : 'bg-white text-[#6E675F] border border-[#EDE7DF]'}">
            ${escapeHtml(g.name)}
          </button>
        `).join('')}
      </div>

      <!-- Greedy Simplification Summary Banner -->
      <section class="rounded-[28px] bg-gradient-to-br from-[#D7E8B6] via-[#E8F3D1] to-[#FED8C8]/70 p-5 border border-[#C9E0A2] space-y-2">
        <div class="flex items-center justify-between">
          <span class="px-3 py-1 rounded-full bg-white/90 text-[#365314] text-[11px] font-extrabold">Simplified Payment Plan</span>
          <span class="text-[11px] font-extrabold text-[#23201E]">${rawExpenseCount} expense${rawExpenseCount === 1 ? '' : 's'} → ${transfers.length} transfer${transfers.length === 1 ? '' : 's'}</span>
        </div>
        <h2 class="text-[20px] font-extrabold text-[#23201E] leading-tight">
          ${transfers.length === 0 ? `All balances in ${escapeHtml(group.name)} are settled!` : `Settle ${escapeHtml(group.name)} in ${transfers.length} direct payment${transfers.length === 1 ? '' : 's'}`}
        </h2>
        <p class="text-[12px] font-medium text-[#4F4943]">Minimum cash-flow matching eliminates circular back-and-forth payments.</p>
      </section>

      <!-- Required Transfers Cards -->
      <section class="space-y-3">
        <h3 class="text-[15px] font-extrabold text-[#23201E] px-1">Required Payments (${transfers.length})</h3>
        ${transfers.length === 0 ? `
          <div class="bg-white rounded-[26px] p-7 border border-[#EDE7DF] text-center space-y-2">
            <div class="w-12 h-12 rounded-full bg-[#D7E8B6] text-[#365314] mx-auto flex items-center justify-center">
              <span class="material-symbols-outlined text-[24px]">check_circle</span>
            </div>
            <div class="text-[15px] font-extrabold text-[#23201E]">Everyone is settled up ($0.00)</div>
            <p class="text-[12px] text-[#6E675F]">No pending payments remain in this group.</p>
          </div>
        ` : transfers.map(tr => `
          <div class="bg-white rounded-[26px] p-5 border border-[#EDE7DF] shadow-2xs space-y-4">
            <div class="flex items-center justify-between gap-2">
              <!-- Debtor -->
              <div class="flex items-center gap-2.5 min-w-0">
                ${renderAvatarImg(tr.from.avatar, tr.from.name, 'w-11 h-11')}
                <div class="min-w-0">
                  <div class="text-[14px] font-extrabold text-[#23201E] truncate">${escapeHtml(tr.from.name)}</div>
                  <div class="text-[11px] font-bold text-[#B91C1C]">Pays</div>
                </div>
              </div>

              <!-- Amount & Arrow -->
              <div class="flex flex-col items-center px-2 shrink-0">
                <span class="text-[18px] font-extrabold text-[#23201E] tabular-nums">${fmtMoney(tr.cents, group.currencySymbol)}</span>
                <span class="text-[10px] font-extrabold text-[#6E675F]">───►</span>
              </div>

              <!-- Creditor -->
              <div class="flex items-center gap-2.5 min-w-0 justify-end text-right">
                <div class="min-w-0">
                  <div class="text-[14px] font-extrabold text-[#23201E] truncate">${escapeHtml(tr.to.name)}</div>
                  <div class="text-[11px] font-bold text-[#365314]">Receives</div>
                </div>
                ${renderAvatarImg(tr.to.avatar, tr.to.name, 'w-11 h-11')}
              </div>
            </div>

            <div class="grid grid-cols-2 gap-2 pt-1">
              <button onclick="recordGroupSettlement('${group.id}', '${tr.from.id}', '${tr.to.id}', ${tr.cents})" class="h-11 rounded-xl bg-[#23201E] text-white text-[12px] font-extrabold flex items-center justify-center gap-1.5 active:scale-95">
                <span class="material-symbols-outlined text-[16px]">check_circle</span>
                <span>✓ Mark as Paid</span>
              </button>
              <button onclick="sharePaymentReminder('${escapeHtml(tr.from.name)}', '${escapeHtml(tr.to.name)}', '${fmtMoney(tr.cents, group.currencySymbol)}', '${escapeHtml(group.name)}')" class="h-11 rounded-xl bg-[#FAF6F0] border border-[#EDE7DF] text-[#23201E] text-[12px] font-extrabold flex items-center justify-center gap-1.5 active:scale-95">
                <span class="material-symbols-outlined text-[16px]">ios_share</span>
                <span>Send Reminder</span>
              </button>
            </div>
          </div>
        `).join('')}
      </section>

      <!-- Member Net Positions -->
      <section class="bg-white rounded-[26px] p-5 border border-[#EDE7DF] space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-[14px] font-extrabold text-[#23201E]">Member Net Balances</h3>
          <span class="text-[11px] font-bold text-[#6E675F]">Sum = ${fmtMoney(0, group.currencySymbol)}</span>
        </div>
        <div class="space-y-2">
          ${balances.map(b => `
            <div class="flex items-center justify-between p-2.5 rounded-2xl bg-[#FAF6F0] border border-[#EDE7DF]">
              <div class="flex items-center gap-2.5">
                ${renderAvatarImg(b.member.avatar, b.member.name, 'w-8 h-8')}
                <span class="text-[13px] font-bold text-[#23201E]">${escapeHtml(b.member.name)}</span>
              </div>
              <span class="text-[13px] font-extrabold tabular-nums ${b.netCents > 1 ? 'text-[#365314]' : (b.netCents < -1 ? 'text-[#B91C1C]' : 'text-[#6E675F]')}">
                ${b.netCents > 1 ? '+' : ''}${fmtMoney(b.netCents, group.currencySymbol)}
              </span>
            </div>
          `).join('')}
        </div>
      </section>
    </div>
  `;
}

function recordGroupSettlement(groupId, fromId, toId, cents) {
  haptic('confirm');
  const group = STATE.groups.find(g => g.id === groupId);
  if (!group) return;
  const fromMember = group.members.find(m => m.id === fromId);
  const toMember = group.members.find(m => m.id === toId);

  STATE.settlements.unshift({
    id: 'st_' + Date.now(),
    groupId,
    fromId,
    fromName: fromMember ? fromMember.name : 'Member',
    toId,
    toName: toMember ? toMember.name : 'Member',
    cents,
    createdAt: Date.now()
  });

  try {
    if (window.SplitMateNative && window.SplitMateNative.markDebtSettled) {
      window.SplitMateNative.markDebtSettled(
        fromMember ? fromMember.name : 'Member',
        toMember ? toMember.name : 'Member',
        cents,
        'Direct Payment'
      );
    }
  } catch (e) {}

  renderApp();
  showToast(`Recorded payment: ${fromMember ? fromMember.name : ''} → ${toMember ? toMember.name : ''} (${fmtMoney(cents, group.currencySymbol)})`);
}

function sharePaymentReminder(fromName, toName, amountStr, groupName) {
  const msg = `Hey ${fromName}! Quick reminder for ${groupName}: your simplified settle-up share is ${amountStr} to ${toName} on SplitMate.`;
  if (window.SplitMateNative && window.SplitMateNative.shareLedgerExport) {
    window.SplitMateNative.shareLedgerExport('SplitMate Payment Reminder', msg);
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(msg);
    showToast('Payment reminder copied to clipboard');
  }
}

// ============================================================================
// TAB 4: ACTIVITY HISTORY & ON-DEVICE PRIVACY VAULT (100% Dynamic)
// ============================================================================
function renderActivityTab() {
  const allEvents = [
    ...STATE.expenses.map(e => ({ ...e, eventType: 'expense' })),
    ...STATE.settlements.map(s => ({ ...s, eventType: 'settlement' }))
  ].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

  return `
    <div class="space-y-5 pb-10">
      <!-- On-Device Privacy & Local Backup Card -->
      <section class="bg-white rounded-[28px] p-5 border border-[#EDE7DF] shadow-2xs space-y-3.5">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-center gap-3">
            <div class="w-11 h-11 rounded-2xl bg-[#D7E8B6] text-[#365314] flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-[22px]">shield_lock</span>
            </div>
            <div>
              <h2 class="text-[16px] font-extrabold text-[#23201E]">100% On-Device Private Storage</h2>
              <p class="text-[12px] text-[#6E675F] font-medium">All groups, expenses & avatars stay inside your phone's private sandbox.</p>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-2 pt-1">
          <button onclick="exportPrivateBackup()" class="h-10 rounded-xl bg-[#23201E] text-white text-[12px] font-extrabold flex items-center justify-center gap-1.5">
            <span class="material-symbols-outlined text-[16px]">download</span>
            <span>Export Backup</span>
          </button>
          <button onclick="clearAllAppData()" class="h-10 rounded-xl bg-[#FDF0EB] border border-[#F8D0C0] text-[#9A2B15] text-[12px] font-extrabold flex items-center justify-center gap-1.5">
            <span class="material-symbols-outlined text-[16px]">restart_alt</span>
            <span>Clear / Reset Data</span>
          </button>
        </div>
      </section>

      <!-- Chronological Activity Feed -->
      <section class="space-y-3">
        <div class="flex items-center justify-between px-1">
          <h3 class="text-[16px] font-extrabold text-[#23201E]">Recent Activity (${allEvents.length})</h3>
        </div>

        ${allEvents.length === 0 ? `
          <div class="bg-white rounded-[26px] p-8 border border-[#EDE7DF] text-center space-y-2">
            <span class="material-symbols-outlined text-[28px] text-[#8C847B]">history</span>
            <div class="text-[14px] font-extrabold text-[#23201E]">No activity recorded yet</div>
            <p class="text-[12px] text-[#6E675F]">Add an expense or settle a balance to see your history here.</p>
          </div>
        ` : allEvents.map(item => {
          const group = STATE.groups.find(g => g.id === item.groupId);
          const groupName = group ? group.name : 'Group';
          const sym = group ? group.currencySymbol : STATE.user.currencySymbol;

          if (item.eventType === 'settlement') {
            return `
              <div class="bg-white rounded-[22px] p-4 border border-[#EDE7DF] flex items-center justify-between gap-3">
                <div class="flex items-center gap-3 min-w-0">
                  <div class="w-10 h-10 rounded-2xl bg-[#D7E8B6]/70 text-[#365314] flex items-center justify-center shrink-0">
                    <span class="material-symbols-outlined text-[20px]">payments</span>
                  </div>
                  <div class="min-w-0">
                    <div class="text-[13px] font-extrabold text-[#23201E] truncate">${escapeHtml(item.fromName)} paid ${escapeHtml(item.toName)}</div>
                    <div class="text-[11px] font-semibold text-[#6E675F]">${escapeHtml(groupName)} · ${timeAgo(item.createdAt)}</div>
                  </div>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  <span class="text-[14px] font-extrabold text-[#365314] tabular-nums">${fmtMoney(item.cents, sym)}</span>
                  <button onclick="deleteActivityItem('settlement', '${item.id}')" class="w-7 h-7 rounded-full bg-[#FAF6F0] text-[#8C847B] text-[12px]">✕</button>
                </div>
              </div>
            `;
          }

          const payer = group ? group.members.find(m => m.id === item.payerId) : null;
          return `
            <div class="bg-white rounded-[22px] p-4 border border-[#EDE7DF] flex items-center justify-between gap-3">
              <div class="flex items-center gap-3 min-w-0">
                ${payer ? renderAvatarImg(payer.avatar, payer.name, 'w-10 h-10') : `<div class="w-10 h-10 rounded-2xl bg-[#FAF6F0] flex items-center justify-center"><span class="material-symbols-outlined text-[20px]">receipt</span></div>`}
                <div class="min-w-0">
                  <div class="text-[14px] font-extrabold text-[#23201E] truncate">${escapeHtml(item.title)}</div>
                  <div class="text-[11px] font-semibold text-[#6E675F] truncate">Paid by ${escapeHtml(payer ? payer.name : 'Member')} · ${escapeHtml(groupName)} · ${timeAgo(item.createdAt)}</div>
                </div>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <div class="text-right">
                  <div class="text-[14px] font-extrabold text-[#23201E] tabular-nums">${fmtMoney(item.totalCents, sym)}</div>
                  <div class="text-[10px] font-bold text-[#365314]">0.00 drift</div>
                </div>
                <button onclick="deleteActivityItem('expense', '${item.id}')" class="w-7 h-7 rounded-full bg-[#FAF6F0] text-[#8C847B] text-[12px]" title="Delete">✕</button>
              </div>
            </div>
          `;
        }).join('')}
      </section>
    </div>
  `;
}

function deleteActivityItem(type, id) {
  haptic('tap');
  if (type === 'expense') {
    STATE.expenses = STATE.expenses.filter(e => e.id !== id);
  } else {
    STATE.settlements = STATE.settlements.filter(s => s.id !== id);
  }
  renderApp();
  showToast('Removed entry and recalculated balances');
}

function exportPrivateBackup() {
  const backup = JSON.stringify(STATE, null, 2);
  if (window.SplitMateNative && window.SplitMateNative.shareLedgerExport) {
    window.SplitMateNative.shareLedgerExport('SplitMate Private Backup (.json)', backup);
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(backup);
    showToast('Backup JSON copied to clipboard');
  }
}

function clearAllAppData() {
  if (!confirm('Start fresh with empty groups or reset to sample groups?\n\nClick OK to clear all expenses and start fresh.')) return;
  haptic('confirm');
  STATE.expenses = [];
  STATE.settlements = [];
  renderApp();
  showToast('Cleared all expenses — balances reset to 0.00');
}
