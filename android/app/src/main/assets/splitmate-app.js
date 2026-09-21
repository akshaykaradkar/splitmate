/**
 * SplitMate v2.0 — 100% Dynamic, Privacy-First Group Expense & Receipt Engine
 *
 * - Zero static hardcoded screens: Every group, member, avatar, currency, expense,
 *   receipt claim, greedy debt transfer, and activity log is dynamically rendered from
 *   live state persisted privately on the user's phone (`SplitMateNative` + `localStorage`).
 * - 161 World Currencies (`vendor/currencies.json` + live `open.er-api.com` exchange rates).
 * - Illustrated Face & Upper-Body Character Art (`avatars/peep_*.svg`, `avatars/micah_*.svg` + DiceBear API).
 * - Exact Mathematical Engines (BRD):
 *   1. Integer-cent storage (`Math.round(amount * 100)`)
 *   2. Locked Proportional Tax/Tip Multiplier (`m = Total / Subtotal`)
 *   3. Largest Remainder Method (`0.00` penny rounding drift)
 *   4. Greedy Minimum Cash Flow Debt Simplification (Max-Priority Queue matching)
 */

const AVATAR_GALLERY = [
  { id: 'peep_1', url: 'avatars/peep_1.svg', label: 'Character 1' },
  { id: 'peep_2', url: 'avatars/peep_2.svg', label: 'Character 2' },
  { id: 'peep_3', url: 'avatars/peep_3.svg', label: 'Character 3' },
  { id: 'peep_4', url: 'avatars/peep_4.svg', label: 'Character 4' },
  { id: 'peep_5', url: 'avatars/peep_5.svg', label: 'Character 5' },
  { id: 'peep_6', url: 'avatars/peep_6.svg', label: 'Character 6' },
  { id: 'micah_1', url: 'avatars/micah_1.svg', label: 'Portrait 1' },
  { id: 'micah_2', url: 'avatars/micah_2.svg', label: 'Portrait 2' },
  { id: 'micah_3', url: 'avatars/micah_3.svg', label: 'Portrait 3' },
  { id: 'micah_4', url: 'avatars/micah_4.svg', label: 'Portrait 4' },
  { id: 'micah_5', url: 'avatars/micah_5.svg', label: 'Portrait 5' },
  { id: 'micah_6', url: 'avatars/micah_6.svg', label: 'Portrait 6' }
];

const GROUP_ICONS = [
  { icon: 'flight_takeoff', label: 'Trip' },
  { icon: 'home', label: 'Home' },
  { icon: 'restaurant', label: 'Dining' },
  { icon: 'cabin', label: 'Getaway' },
  { icon: 'celebration', label: 'Event' },
  { icon: 'shopping_bag', label: 'Shopping' }
];

let WORLD_CURRENCIES = [
  { code: 'INR', name: 'Indian Rupee', symbol: '₹', country: 'India', flag: '🇮🇳', cca2: 'in', rateFromUSD: 83.95 },
  { code: 'USD', name: 'US Dollar', symbol: '$', country: 'United States', flag: '🇺🇸', cca2: 'us', rateFromUSD: 1.0 },
  { code: 'EUR', name: 'Euro', symbol: '€', country: 'Eurozone', flag: '🇪🇺', cca2: 'eu', rateFromUSD: 0.92 },
  { code: 'GBP', name: 'British Pound', symbol: '£', country: 'United Kingdom', flag: '🇬🇧', cca2: 'gb', rateFromUSD: 0.79 },
  { code: 'AED', name: 'UAE Dirham', symbol: 'د.إ', country: 'United Arab Emirates', flag: '🇦🇪', cca2: 'ae', rateFromUSD: 3.67 },
  { code: 'SGD', name: 'Singapore Dollar', symbol: 'S$', country: 'Singapore', flag: '🇸🇬', cca2: 'sg', rateFromUSD: 1.34 },
  { code: 'JPY', name: 'Japanese Yen', symbol: '¥', country: 'Japan', flag: '🇯🇵', cca2: 'jp', rateFromUSD: 151.2 }
];

function createDefaultState(userName = 'Akshay', userAvatar = 'avatars/peep_1.svg', currencyCode = 'INR') {
  const curr = WORLD_CURRENCIES.find(c => c.code === currencyCode) || WORLD_CURRENCIES[0];
  return {
    onboarded: false,
    activeTab: 'groups', // 'groups' | 'split' | 'settle' | 'activity'
    activeGroupId: 'g_goa',
    groupFilter: 'all', // 'all' | 'owed' | 'owe' | 'settled'
    user: {
      id: 'm_me',
      name: userName,
      avatar: userAvatar,
      currencyCode: curr.code,
      currencySymbol: curr.symbol,
      currencyName: curr.name,
      currencyFlag: curr.flag
    },
    groups: [
      {
        id: 'g_goa',
        name: 'Weekend Getaway',
        icon: 'flight_takeoff',
        currencyCode: curr.code,
        currencySymbol: curr.symbol,
        createdAt: Date.now() - 86400000 * 3,
        members: [
          { id: 'm_me', name: userName, avatar: userAvatar, isMe: true },
          { id: 'm_2', name: 'Rohan', avatar: 'avatars/peep_2.svg', isMe: false },
          { id: 'm_3', name: 'Priya', avatar: 'avatars/peep_3.svg', isMe: false },
          { id: 'm_4', name: 'Kabir', avatar: 'avatars/micah_1.svg', isMe: false }
        ]
      },
      {
        id: 'g_apt',
        name: 'Apartment & Utilities',
        icon: 'home',
        currencyCode: curr.code,
        currencySymbol: curr.symbol,
        createdAt: Date.now() - 86400000 * 7,
        members: [
          { id: 'm_me', name: userName, avatar: userAvatar, isMe: true },
          { id: 'm_2', name: 'Rohan', avatar: 'avatars/peep_2.svg', isMe: false },
          { id: 'm_5', name: 'Sneha', avatar: 'avatars/micah_3.svg', isMe: false }
        ]
      }
    ],
    expenses: [
      {
        id: 'exp_1',
        groupId: 'g_goa',
        title: 'Villa Booking & Groceries',
        category: 'Stay',
        payerId: 'm_me',
        totalCents: 640000, // 6,400.00
        createdAt: Date.now() - 3600000 * 18,
        type: 'expense',
        splits: [
          { memberId: 'm_me', cents: 160000, plusOneCent: false },
          { memberId: 'm_2', cents: 160000, plusOneCent: false },
          { memberId: 'm_3', cents: 160000, plusOneCent: false },
          { memberId: 'm_4', cents: 160000, plusOneCent: false }
        ]
      },
      {
        id: 'exp_2',
        groupId: 'g_apt',
        title: 'High-Speed Fiber Wifi',
        category: 'Utilities',
        payerId: 'm_2',
        totalCents: 145000, // 1,450.00 -> 483.34, 483.33, 483.33
        createdAt: Date.now() - 3600000 * 5,
        type: 'expense',
        splits: [
          { memberId: 'm_me', cents: 48334, plusOneCent: true },
          { memberId: 'm_2', cents: 48333, plusOneCent: false },
          { memberId: 'm_5', cents: 48333, plusOneCent: false }
        ]
      }
    ],
    settlements: [],
    // Live Receipt Claim workspace state (`Split Bill` tab)
    receiptDraft: {
      groupId: 'g_goa',
      title: 'Beachfront Dinner & Drinks',
      payerId: 'm_me',
      activeClaimerId: 'm_me',
      taxPercent: 5.0,
      tipPercent: 10.0,
      items: [
        { id: 'ri_1', name: 'Wood-Fired Truffle Pizza', cents: 68000, claimedBy: ['m_me', 'm_3'] },
        { id: 'ri_2', name: 'Grilled Sea Bass Platter', cents: 85000, claimedBy: ['m_2'] },
        { id: 'ri_3', name: 'Artisanal Pasta Arrabbiata', cents: 54000, claimedBy: ['m_4'] },
        { id: 'ri_4', name: 'Shared Garlic Bread & Mezze', cents: 42000, claimedBy: [] }
      ]
    }
  };
}

let STATE = createDefaultState();

// --- Haptics & Native Storage Bridge ---
function haptic(type = 'tap') {
  try {
    if (window.SplitMateNative && window.SplitMateNative.triggerHaptic) {
      window.SplitMateNative.triggerHaptic(type);
    }
  } catch (e) {}
}

function saveState() {
  const raw = JSON.stringify(STATE);
  try {
    localStorage.setItem('splitmate_dynamic_state_v2', raw);
  } catch (e) {}
  try {
    if (window.SplitMateNative && window.SplitMateNative.savePrivateDeviceState) {
      window.SplitMateNative.savePrivateDeviceState(raw);
    }
  } catch (e) {}
}

function loadState() {
  try {
    let raw = '';
    if (window.SplitMateNative && window.SplitMateNative.loadPrivateDeviceState) {
      raw = window.SplitMateNative.loadPrivateDeviceState();
    }
    if (!raw) {
      raw = localStorage.getItem('splitmate_dynamic_state_v2') || '';
    }
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && parsed.user && Array.isArray(parsed.groups)) {
        STATE = parsed;
      }
    }
  } catch (e) {}
}

function showToast(msg, tone = 'olive') {
  let old = document.getElementById('sm-toast');
  if (old) old.remove();
  const el = document.createElement('div');
  el.id = 'sm-toast';
  const bg = tone === 'terracotta' ? '#7C2D12' : '#23201E';
  const pillBg = tone === 'terracotta' ? '#FED8C8' : '#D7E8B6';
  const pillText = tone === 'terracotta' ? '#7C2D12' : '#365314';
  el.style.cssText = `
    position:fixed; bottom:84px; left:50%; transform:translateX(-50%) translateY(16px) scale(0.94);
    background:${bg}; color:#FAF6F0; padding:10px 16px; border-radius:9999px;
    font-size:12px; font-weight:700; display:flex; align-items:center; gap:8px;
    box-shadow:0 10px 28px rgba(35,32,30,0.22); z-index:9999; opacity:0;
    transition:all 320ms cubic-bezier(0.05, 0.7, 0.1, 1); pointer-events:none; max-width:92vw;
  `;
  el.innerHTML = `<span style="background:${pillBg};color:${pillText};padding:2px 8px;border-radius:999px;font-size:10px;font-weight:800;flex-shrink:0;">✓</span><span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${msg}</span>`;
  document.body.appendChild(el);
  requestAnimationFrame(() => {
    el.style.opacity = '1';
    el.style.transform = 'translateX(-50%) translateY(0) scale(1)';
  });
  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateX(-50%) translateY(8px) scale(0.96)';
    setTimeout(() => el.remove(), 320);
  }, 2300);
}

// --- Formatters & Mathematical Engine ---
function fmtMoney(cents, symbolOverride) {
  const sym = symbolOverride || STATE.user.currencySymbol || '₹';
  const abs = Math.abs(Math.round(cents)) / 100;
  const formatted = abs.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return (cents < 0 ? '-' : '') + sym + formatted;
}

/**
 * Largest Remainder Method (0.00¢ drift)
 * Deterministically distributes integer cents across N member IDs.
 */
function splitCentsZeroDrift(totalCents, memberIds) {
  if (!memberIds || memberIds.length === 0) return [];
  const n = memberIds.length;
  const baseFloor = Math.floor(totalCents / n);
  const rem = totalCents % n;
  return memberIds.map((id, idx) => ({
    memberId: id,
    cents: baseFloor + (idx < rem ? 1 : 0),
    plusOneCent: idx < rem
  }));
}

/**
 * Calculates net balance (in cents) for each member inside a specific group.
 * Net Balance = Total Paid by Member - Total Owed by Member + Settlements Paid - Settlements Received.
 */
function computeGroupMemberBalances(groupId) {
  const group = STATE.groups.find(g => g.id === groupId);
  if (!group) return [];
  const map = {};
  group.members.forEach(m => {
    map[m.id] = { member: m, netCents: 0 };
  });

  // 1. Add expenses
  STATE.expenses.filter(e => e.groupId === groupId).forEach(exp => {
    if (map[exp.payerId]) {
      map[exp.payerId].netCents += exp.totalCents;
    }
    (exp.splits || []).forEach(sp => {
      if (map[sp.memberId]) {
        map[sp.memberId].netCents -= sp.cents;
      }
    });
  });

  // 2. Apply recorded settlements
  STATE.settlements.filter(s => s.groupId === groupId).forEach(st => {
    if (map[st.fromId]) map[st.fromId].netCents += st.cents;
    if (map[st.toId]) map[st.toId].netCents -= st.cents;
  });

  return Object.values(map);
}

/**
 * Greedy Minimum Cash Flow Debt Simplification (Max-Priority Queue Matching)
 * Reduces raw debts to at most N - 1 direct transfers.
 */
function simplifyGroupDebtsGreedy(groupId) {
  const balances = computeGroupMemberBalances(groupId);
  const creditors = [];
  const debtors = [];

  balances.forEach(b => {
    if (b.netCents > 1) creditors.push({ member: b.member, amount: b.netCents });
    else if (b.netCents < -1) debtors.push({ member: b.member, amount: -b.netCents });
  });

  const transfers = [];
  while (creditors.length > 0 && debtors.length > 0) {
    creditors.sort((a, b) => b.amount - a.amount);
    debtors.sort((a, b) => b.amount - a.amount);

    const maxCreditor = creditors[0];
    const maxDebtor = debtors[0];
    const settled = Math.min(maxCreditor.amount, maxDebtor.amount);

    transfers.push({
      from: maxDebtor.member,
      to: maxCreditor.member,
      cents: settled
    });

    maxCreditor.amount -= settled;
    maxDebtor.amount -= settled;
    if (maxCreditor.amount <= 1) creditors.shift();
    if (maxDebtor.amount <= 1) debtors.shift();
  }
  return { balances, transfers };
}

// --- Load 161 World Currencies & Live Exchange Rates ---
async function initCurrencies() {
  try {
    const res = await fetch('vendor/currencies.json');
    if (res.ok) {
      const list = await res.json();
      if (Array.isArray(list) && list.length > 10) {
        WORLD_CURRENCIES = list;
      }
    }
  } catch (e) {}

  // Also refresh live exchange rates from open-source Open ER API if online
  try {
    const liveRes = await fetch('https://open.er-api.com/v6/latest/USD');
    if (liveRes.ok) {
      const data = await liveRes.json();
      if (data && data.rates) {
        WORLD_CURRENCIES.forEach(c => {
          if (data.rates[c.code]) c.rateFromUSD = data.rates[c.code];
        });
      }
    }
  } catch (e) {}
}

// --- Generate Illustrated Face/Body Avatar URL from Name (DiceBear Open-Peeps / Micah API with Offline Fallback) ---
function getDiceBearUrl(seed, style = 'open-peeps') {
  const clean = encodeURIComponent((seed || 'User').trim());
  const bgs = ['d7e8b6', 'fed8c8', 'dce3fd', 'fde68a', 'fbcfe8', 'bae6fd'];
  let hash = 0;
  for (let i = 0; i < clean.length; i++) hash = clean.charCodeAt(i) + ((hash << 5) - hash);
  const bg = bgs[Math.abs(hash) % bgs.length];
  return `https://api.dicebear.com/9.x/${style}/svg?seed=${clean}&backgroundColor=${bg}`;
}

function getFallbackLocalAvatar(seed) {
  let hash = 0;
  const s = (seed || 'U').trim();
  for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash);
  return AVATAR_GALLERY[Math.abs(hash) % AVATAR_GALLERY.length].url;
}
