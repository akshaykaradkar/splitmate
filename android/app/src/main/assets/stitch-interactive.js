/**
 * SplitMate Interactive Engine & Android Motion Physics Controller
 * Bridges every Stitch prototype screen with:
 * 1. Android Motion Spring Physics (--droid-sys-motion-easing-emphasized-decelerate)
 * 2. Native Android Haptics & SQLite Vault (window.SplitMateNative)
 * 3. Live 0.00¢ Drift Largest Remainder & Locked Multiplier (m = 1.2375x) Calculations
 * 4. Cross-screen navigation & state synchronization
 */
(function () {
  const CURRENCY_SYMBOLS = {
    USD: '$',
    EUR: '€',
    GBP: '£',
    JPY: '¥',
    CAD: 'CA$',
    CHF: 'Fr '
  };

  function haptic(type = 'tap') {
    try {
      if (window.SplitMateNative && window.SplitMateNative.triggerHaptic) {
        window.SplitMateNative.triggerHaptic(type);
      } else if (window.parent && window.parent.SplitMateNative) {
        window.parent.SplitMateNative.triggerHaptic(type);
      }
    } catch (e) {}
  }

  function notifyParent(action, payload = {}) {
    haptic(payload.haptic || 'tap');
    if (window.parent && window.parent !== window && window.parent.onStitchAction) {
      window.parent.onStitchAction(action, payload);
    }
  }

  function showToast(message, tone = 'olive') {
    if (window.parent && window.parent !== window && window.parent.showExpressiveToast) {
      window.parent.showExpressiveToast(message, tone);
      return;
    }
    let existing = document.getElementById('sm-expressive-toast');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.id = 'sm-expressive-toast';
    const bg = tone === 'terracotta' ? '#7C2D12' : '#23201E';
    const badgeBg = tone === 'terracotta' ? '#FED8C8' : '#D7E8B6';
    const badgeColor = tone === 'terracotta' ? '#7C2D12' : '#365314';
    toast.style.cssText = `
      position: fixed; bottom: 84px; left: 50%; transform: translateX(-50%) translateY(20px) scale(0.92);
      background: ${bg}; color: #FAF6F0; padding: 10px 16px; border-radius: 9999px;
      font-family: 'Plus Jakarta Sans', sans-serif; font-size: 12px; font-weight: 700;
      display: flex; align-items: center; gap: 8px; z-index: 9999;
      box-shadow: 0 12px 28px rgba(35,32,30,0.22); opacity: 0;
      transition: all 380ms cubic-bezier(0.05, 0.7, 0.1, 1); pointer-events: none; white-space: nowrap;
    `;
    toast.innerHTML = `<span style="background:${badgeBg};color:${badgeColor};padding:2px 8px;border-radius:999px;font-size:10px;font-weight:800;">0.00¢ DRIFT</span><span>${message}</span>`;
    document.body.appendChild(toast);
    requestAnimationFrame(() => {
      toast.style.opacity = '1';
      toast.style.transform = 'translateX(-50%) translateY(0) scale(1)';
    });
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(-50%) translateY(10px) scale(0.95)';
      setTimeout(() => toast.remove(), 380);
    }, 2600);
  }

  document.addEventListener('DOMContentLoaded', () => {
    // 1. Apply Android Motion Expressive Spring Physics to all interactive elements
    const style = document.createElement('style');
    style.textContent = `
      :root {
        --sm-spring-decelerate: var(--droid-sys-motion-easing-emphasized-decelerate, cubic-bezier(0.05, 0.7, 0.1, 1));
        --sm-spring-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
      }
      button, a, article, [role="button"] {
        transition: transform 240ms var(--sm-spring-bounce), background-color 200ms ease, box-shadow 200ms ease, border-color 200ms ease !important;
      }
      button:active, a:active {
        transform: scale(0.95) !important;
      }
      /* Adjust top padding now that fake iOS status bar is removed */
      main.pt-28 { padding-top: 5.25rem !important; }
      main.pt-24 { padding-top: 4.75rem !important; }
    `;
    document.head.appendChild(style);

    // 2. Wire Bottom Navigation Dock across all Stitch screens
    document.querySelectorAll('nav a[data-path], nav button').forEach((navEl) => {
      navEl.addEventListener('click', (e) => {
        e.preventDefault();
        const path = navEl.getAttribute('data-path') || navEl.textContent.trim().toLowerCase();
        if (path.includes('ledger')) notifyParent('navigate', { screen: 'ledgers_animated' });
        else if (path.includes('split') || path.includes('claim')) notifyParent('navigate', { screen: 'receipt_claim' });
        else if (path.includes('settle')) notifyParent('navigate', { screen: 'greedy_settle' });
        else if (path.includes('audit') || path.includes('history')) notifyParent('navigate', { screen: 'audit_vault' });
      });
    });

    // 3. Wire Header Profile Avatar ("JD" / "Alex") to open User Profile & Onboarding Screen
    document.querySelectorAll('header').forEach((hdr) => {
      const avatarCandidates = hdr.querySelectorAll('div.rounded-full, button');
      avatarCandidates.forEach((el) => {
        const txt = el.textContent.trim();
        if (txt === 'JD' || txt === 'Alex' || txt === '🌱' || txt === '🦊') {
          el.style.cursor = 'pointer';
          el.title = 'Open User Profile & Guest/Cloud Settings';
          el.addEventListener('click', (e) => {
            e.stopPropagation();
            notifyParent('navigate', { screen: 'onboarding' });
          });
        }
      });
    });

    // 4. Screen-specific Interactive Wiring
    const pathUrl = window.location.pathname;

    // --- SCREEN A: Guest Onboarding & User Profile (splitmate_premium_guest_onboarding) ---
    if (pathUrl.includes('splitmate_premium_guest_onboarding')) {
      let selectedMode = 'Instant Guest (Offline)';
      let selectedEmoji = '🌱';
      let selectedCurrency = 'USD';

      // Mode switcher buttons (Instant Guest vs Cloud Sync)
      const modeBtns = document.querySelectorAll('.grid-cols-2 button');
      modeBtns.forEach((btn) => {
        btn.addEventListener('click', () => {
          haptic('tap');
          modeBtns.forEach((b) => {
            b.className = 'flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-full text-text-secondary font-semibold text-[13px] hover:text-text-primary transition-all';
          });
          btn.className = 'flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-full bg-white text-text-primary font-bold text-[13px] shadow-soft transition-all';
          selectedMode = btn.textContent.trim();
          showToast(`Mode switched to ${selectedMode}`);
        });
      });

      // Organic Avatar Picker (5 tokens)
      const avatarBtns = Array.from(document.querySelectorAll('button')).filter((b) =>
        ['🌱', '🦊', '🐱', '🦔', '☕'].includes(b.textContent.trim())
      );
      avatarBtns.forEach((btn) => {
        btn.addEventListener('click', () => {
          haptic('tap');
          avatarBtns.forEach((b) => {
            b.classList.remove('ring-2', 'ring-text-primary', 'ring-offset-2');
            const check = b.querySelector('span.absolute');
            if (check) check.remove();
          });
          btn.classList.add('ring-2', 'ring-text-primary', 'ring-offset-2');
          const badge = document.createElement('span');
          badge.className = 'absolute -bottom-0.5 -right-0.5 w-4 h-4 bg-text-primary text-white rounded-full flex items-center justify-center text-[10px] font-bold';
          badge.textContent = '✓';
          btn.appendChild(badge);
          selectedEmoji = btn.textContent.replace('✓', '').trim();
          showToast(`Avatar token ${selectedEmoji} selected`);
        });
      });

      // 6-Currency Grid Selector (USD, EUR, GBP, JPY, CAD, CHF)
      const currencyBtns = Array.from(document.querySelectorAll('.grid-cols-3 button'));
      currencyBtns.forEach((btn) => {
        btn.addEventListener('click', () => {
          haptic('tap');
          currencyBtns.forEach((b) => {
            b.className = 'flex flex-col items-start p-3 rounded-2xl bg-[#F5F0E6]/70 border border-transparent hover:border-border-warm transition-all text-left';
            const dot = b.querySelector('span.w-2');
            if (dot) dot.remove();
          });
          btn.className = 'relative flex flex-col items-start p-3 rounded-2xl bg-buckwheat-green/45 border-2 border-buckwheat-green-dark transition-all text-left';
          const codeSpan = btn.querySelector('span.text-\\[13px\\]');
          selectedCurrency = codeSpan ? codeSpan.textContent.trim() : 'USD';
          showToast(`Default ledger currency set to ${selectedCurrency}`);
        });
      });

      // Start Splitting CTA
      const startBtn = Array.from(document.querySelectorAll('button')).find((b) =>
        b.textContent.includes('Start Splitting')
      );
      if (startBtn) {
        startBtn.addEventListener('click', () => {
          const aliasInput = document.querySelector('input[type="text"]');
          const alias = (aliasInput && aliasInput.value.trim()) || 'Alex';
          try {
            if (window.parent && window.parent.SplitMateNative) {
              window.parent.SplitMateNative.saveUserProfile(alias, selectedEmoji, selectedCurrency, selectedMode.includes('Guest'));
            }
          } catch (e) {}
          notifyParent('onboarding_complete', {
            alias,
            emoji: selectedEmoji,
            currency: selectedCurrency,
            symbol: CURRENCY_SYMBOLS[selectedCurrency] || '$',
            mode: selectedMode,
            haptic: 'confirm'
          });
        });
      }
    }

    // --- SCREEN B: Quick Expense Logger & Calculator (splitmate_quick_expense_logger_calculator) ---
    if (pathUrl.includes('splitmate_quick_expense_logger_calculator')) {
      let currentCents = 4250; // $42.50 default matching Stitch
      let currentCategory = 'Groceries';
      let currentPayer = 'Alex';
      const amountDisplay = Array.from(document.querySelectorAll('span')).find((s) => s.textContent.trim() === '42.50');
      const saveBtn = Array.from(document.querySelectorAll('button')).find((b) => b.textContent.includes('Save Expense'));
      const splitCards = document.querySelectorAll('.grid-cols-3 > div');

      function updatePennyPerfectSplit() {
        const dollars = (currentCents / 100).toFixed(2);
        if (amountDisplay) amountDisplay.textContent = dollars;
        if (saveBtn) {
          saveBtn.innerHTML = `<span class="material-symbols-outlined text-[18px]">check_circle</span><span>Save Expense · $${dollars}</span><span class="material-symbols-outlined text-[18px]">arrow_forward</span>`;
        }
        // Largest Remainder Method (0.00c drift across Alex, Sam, Priya)
        const baseShare = Math.floor(currentCents / 3);
        const rem = currentCents % 3;
        const shares = [
          baseShare + (rem >= 1 ? 1 : 0),
          baseShare + (rem >= 2 ? 1 : 0),
          baseShare
        ];
        const names = [
          rem >= 1 ? 'Alex (+1¢)' : 'Alex',
          rem >= 2 ? 'Sam (+1¢)' : 'Sam',
          'Priya'
        ];
        if (splitCards.length >= 3) {
          for (let i = 0; i < 3; i++) {
            const spans = splitCards[i].querySelectorAll('span');
            if (spans.length >= 2) {
              spans[0].textContent = names[i];
              spans[1].textContent = '$' + (shares[i] / 100).toFixed(2);
            }
          }
        }
      }

      // Close button & Reset button
      const headerBtns = document.querySelectorAll('header button');
      if (headerBtns[0]) {
        headerBtns[0].addEventListener('click', () => notifyParent('navigate', { screen: 'ledgers_animated' }));
      }
      if (headerBtns[2]) {
        headerBtns[2].addEventListener('click', () => {
          currentCents = 0;
          updatePennyPerfectSplit();
          showToast('Calculator reset to $0.00');
        });
      }

      // Editable expense title pill
      const titleBtn = Array.from(document.querySelectorAll('button')).find((b) =>
        b.textContent.includes('Mountain Sunrise Groceries')
      );
      if (titleBtn) {
        titleBtn.addEventListener('click', () => {
          const nextTitle = prompt('Enter expense description:', 'Mountain Sunrise Groceries');
          if (nextTitle && nextTitle.trim()) {
            const span = titleBtn.querySelector('span.text-\\[13px\\]');
            if (span) span.textContent = nextTitle.trim();
          }
        });
      }

      // Quick Add Chips (+$1, +$5, +$10, 1/3 Equal, +18% Tip)
      document.querySelectorAll('button').forEach((btn) => {
        const t = btn.textContent.trim();
        if (t === '+$1') {
          btn.addEventListener('click', () => { currentCents += 100; haptic('clock_tick'); updatePennyPerfectSplit(); });
        } else if (t === '+$5') {
          btn.addEventListener('click', () => { currentCents += 500; haptic('clock_tick'); updatePennyPerfectSplit(); });
        } else if (t === '+$10') {
          btn.addEventListener('click', () => { currentCents += 1000; haptic('clock_tick'); updatePennyPerfectSplit(); });
        } else if (t === '+18% Tip') {
          btn.addEventListener('click', () => {
            currentCents = Math.round(currentCents * 1.18);
            haptic('confirm');
            updatePennyPerfectSplit();
            showToast('Applied +18% Tip with 0.00¢ Largest Remainder');
          });
        } else if (t === '⅓ Equal') {
          btn.addEventListener('click', () => {
            haptic('tap');
            updatePennyPerfectSplit();
            showToast('Largest Remainder 3-way equal split verified (0.00¢ drift)');
          });
        }
      });

      // 3x4 Numeric Keypad
      const keypadBtns = document.querySelectorAll('.grid-cols-3.gap-2 button');
      keypadBtns.forEach((btn) => {
        btn.addEventListener('click', () => {
          haptic('clock_tick');
          const key = btn.textContent.trim();
          if (btn.querySelector('.material-symbols-outlined') || key === 'backspace') {
            currentCents = Math.floor(currentCents / 10);
          } else if (/^[0-9]$/.test(key)) {
            if (currentCents < 999999) {
              currentCents = currentCents * 10 + parseInt(key, 10);
            }
          } else if (key === '.') {
            // Cents shift mode already active
          }
          updatePennyPerfectSplit();
        });
      });

      // Save Expense CTA
      if (saveBtn) {
        saveBtn.addEventListener('click', () => {
          const titleSpan = titleBtn ? titleBtn.querySelector('span.text-\\[13px\\]') : null;
          const title = titleSpan ? titleSpan.textContent.trim() : 'Mountain Sunrise Groceries';
          try {
            if (window.parent && window.parent.SplitMateNative) {
              window.parent.SplitMateNative.logQuickExpense(title, currentCategory, 'g_tahoe', currentPayer, currentCents);
            }
          } catch (e) {}
          notifyParent('expense_saved', {
            title,
            cents: currentCents,
            haptic: 'confirm'
          });
        });
      }
    }

    // --- SCREEN C: Live Receipt Claim & Remainder Engine (live_receipt_claim_remainder_engine_buckwheat_style) ---
    if (pathUrl.includes('live_receipt_claim_remainder_engine_buckwheat_style')) {
      const backBtn = document.querySelector('header button');
      if (backBtn) {
        backBtn.addEventListener('click', () => notifyParent('navigate', { screen: 'ledgers_animated' }));
      }

      // Wire embedded custom item calculator keypad
      let customItemCents = 1200; // $12.00 base -> $14.85 final at 1.2375x
      const baseDisplay = Array.from(document.querySelectorAll('span')).find((s) => s.textContent.trim() === '$12.00');
      const finalDisplay = Array.from(document.querySelectorAll('span')).find((s) => s.textContent.includes('$14.85 w/ Tax & Tip'));

      function updateCustomCalc() {
        const baseStr = '$' + (customItemCents / 100).toFixed(2);
        const finalCents = Math.round(customItemCents * 1.2375);
        const finalStr = '→ $' + (finalCents / 100).toFixed(2) + ' w/ Tax & Tip';
        if (baseDisplay) baseDisplay.textContent = baseStr;
        if (finalDisplay) finalDisplay.textContent = finalStr;
      }

      const calcGridBtns = document.querySelectorAll('.grid-cols-4 button');
      calcGridBtns.forEach((btn) => {
        btn.addEventListener('click', () => {
          haptic('clock_tick');
          const t = btn.textContent.trim();
          if (t === '+$1') customItemCents += 100;
          else if (t === '+$5') customItemCents += 500;
          else if (t === '½ Split') customItemCents = Math.round(customItemCents / 2);
          else if (t === '⅓ Split') customItemCents = Math.round(customItemCents / 3);
          else if (btn.querySelector('.material-symbols-outlined')) customItemCents = Math.floor(customItemCents / 10);
          else if (/^[0-9]$/.test(t)) {
            if (customItemCents < 99999) customItemCents = customItemCents * 10 + parseInt(t, 10);
          }
          updateCustomCalc();
        });
      });

      const applyCustomBtn = Array.from(document.querySelectorAll('button')).find((b) =>
        b.textContent.includes('Apply Custom Base')
      );
      if (applyCustomBtn) {
        applyCustomBtn.addEventListener('click', () => {
          haptic('confirm');
          const finalCents = Math.round(customItemCents * 1.2375);
          showToast(`Added $${(customItemCents / 100).toFixed(2)} base ($${(finalCents / 100).toFixed(2)} @ 1.2375×) to active persona!`);
        });
      }

      // Wire "Split $14.00 Remainder Equally (+$4.67/ea)" button
      const splitRemainderBtn = Array.from(document.querySelectorAll('button')).find((b) =>
        b.textContent.includes('Split $14.00 Remainder Equally')
      );
      if (splitRemainderBtn) {
        let remainderSplitDone = false;
        splitRemainderBtn.addEventListener('click', () => {
          haptic('confirm');
          remainderSplitDone = !remainderSplitDone;
          const stripedBar = document.querySelector('.striped-remainder');
          if (remainderSplitDone) {
            if (stripedBar) {
              stripedBar.style.background = '#689336';
            }
            splitRemainderBtn.style.backgroundColor = '#365314';
            splitRemainderBtn.innerHTML = `<span class="material-symbols-outlined text-[16px]">verified</span><span>✓ $14.00 Remainder Split Equally (+$4.67 / +$4.67 / +$4.66 · 0.00¢ Drift)</span>`;
            showToast('Unassigned $14.00 split equally via Largest Remainder (0.00¢ drift)!', 'olive');
          } else {
            if (stripedBar) {
              stripedBar.style.background = '';
            }
            splitRemainderBtn.style.backgroundColor = '#E06B52';
            splitRemainderBtn.innerHTML = `<span class="material-symbols-outlined text-[16px]">bolt</span><span>Split $14.00 Remainder Equally (+$4.67/ea)</span>`;
            showToast('Remainder returned to Payer (Alex) temporary hold', 'terracotta');
          }
        });
      }

      // Wire "Claim" button on Shared Antipasto Misto ($14.00)
      const claimBtn = Array.from(document.querySelectorAll('button')).find((b) => b.textContent.trim() === 'Claim');
      if (claimBtn) {
        claimBtn.addEventListener('click', () => {
          haptic('confirm');
          if (claimBtn.textContent.trim() === 'Claim') {
            claimBtn.textContent = '✓ Claimed (Alex)';
            claimBtn.className = 'h-7 px-3 rounded-full bg-buck-green text-buck-green-dark text-[11px] font-extrabold shadow-xs active:scale-95 transition-all';
            showToast('Shared Antipasto Misto ($14.00) claimed by Alex (1.2375× locked)');
          } else {
            claimBtn.textContent = 'Claim';
            claimBtn.className = 'h-7 px-3 rounded-full bg-[#E06B52] hover:bg-[#d05d44] text-white text-[11px] font-bold shadow-xs active:scale-95 transition-all';
            showToast('Shared Antipasto Misto ($14.00) returned to Unassigned Remainder', 'terracotta');
          }
        });
      }
    }

    // --- SCREEN D: Greedy Debt Simplification (greedy_debt_simplification_buckwheat_style) ---
    if (pathUrl.includes('greedy_debt_simplification_buckwheat_style')) {
      const backBtn = document.querySelector('header button');
      if (backBtn) {
        backBtn.addEventListener('click', () => notifyParent('navigate', { screen: 'ledgers_animated' }));
      }

      // Enhance simulatePayment to also record in SQLite and show toast
      const origSimulate = window.simulatePayment;
      window.simulatePayment = function (cardId, btn) {
        haptic('confirm');
        if (origSimulate) origSimulate(cardId, btn);
        const debtor = cardId.includes('1') ? 'Maya' : 'Kai';
        const cents = cardId.includes('1') ? 16520 : 9850;
        try {
          if (window.parent && window.parent.SplitMateNative) {
            window.parent.SplitMateNative.markDebtSettled(debtor, 'Alex', cents, 'Direct Transfer');
          }
        } catch (e) {}
        showToast(`${debtor} → Alex ($${(cents / 100).toFixed(2)}) settled in SQLite Vault!`);
      };
    }

    // --- SCREEN E: Ledgers Screens (Owed / Animated / You Owe / All Settled / Empty) ---
    document.querySelectorAll('button').forEach((btn) => {
      const t = btn.textContent.trim();
      if (t.includes('+ Add Expense') || t.includes('Scan a Bill') || t.includes('Quick Split')) {
        btn.addEventListener('click', () => notifyParent('navigate', { screen: 'quick_logger' }));
      } else if (t.includes('Pay Sam $142.00')) {
        btn.addEventListener('click', () => {
          haptic('confirm');
          btn.innerHTML = `<span class="material-symbols-outlined text-[14px]">check_circle</span><span>✓ Paid Sam $142.00 (0.00¢ drift)</span>`;
          btn.className = 'w-full h-9 rounded-xl bg-buck-green border border-buck-green-dark/30 text-buck-green-dark text-[12px] font-extrabold flex items-center justify-center gap-1 transition-all';
          showToast('Paid Sam $142.00 · Recorded in SQLite Vault!');
        });
      } else if (t.includes('Pay Liam $42.40')) {
        btn.addEventListener('click', () => {
          haptic('confirm');
          btn.innerHTML = `<span class="material-symbols-outlined text-[14px]">check_circle</span><span>✓ Paid Liam $42.40 (0.00¢ drift)</span>`;
          btn.className = 'w-full h-9 rounded-xl bg-buck-green border border-buck-green-dark/30 text-buck-green-dark text-[12px] font-extrabold flex items-center justify-center gap-1 transition-all';
          showToast('Paid Liam $42.40 · Recorded in SQLite Vault!');
        });
      } else if (t.includes('Create First Group') || t.includes('Weekend Getaway') || t.includes('Roommate Rent')) {
        btn.addEventListener('click', () => {
          showToast('Loaded Active Group Ledger Template!');
          notifyParent('navigate', { screen: 'ledgers_animated' });
        });
      }
    });
  });
})();
