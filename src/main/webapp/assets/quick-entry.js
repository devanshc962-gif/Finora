(() => {
    const panel = document.querySelector("[data-quick-entry]");
    const expenseForm = document.querySelector("[data-expense-form]");
    if (!panel || !expenseForm) return;

    const input = panel.querySelector("#expense-quick-entry");
    const fillButton = panel.querySelector("[data-quick-entry-fill]");
    const status = panel.querySelector("[data-quick-entry-status]");
    const amountField = expenseForm.elements.namedItem("amount");
    const descriptionField = expenseForm.elements.namedItem("description");
    const categoryField = expenseForm.elements.namedItem("category");
    const dateField = expenseForm.elements.namedItem("spentOn");
    const numberWords = {
        zero: 0, oh: 0, one: 1, two: 2, three: 3, four: 4, five: 5,
        six: 6, seven: 7, eight: 8, nine: 9, ten: 10, eleven: 11,
        twelve: 12, thirteen: 13, fourteen: 14, fifteen: 15, sixteen: 16,
        seventeen: 17, eighteen: 18, nineteen: 19, twenty: 20, thirty: 30,
        forty: 40, fifty: 50, sixty: 60, seventy: 70, eighty: 80, ninety: 90
    };

    function setStatus(message, isError = false) {
        status.textContent = message;
        if (isError) status.dataset.state = "error";
        else delete status.dataset.state;
    }

    function parseNumberWords(text) {
        const tokens = text.toLowerCase().match(/\b(?:zero|oh|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|and)\b/g);
        if (!tokens) return NaN;
        let total = 0;
        let group = 0;
        for (const word of tokens) {
            if (word === "and") continue;
            if (word === "hundred") group = (group || 1) * 100;
            else if (word === "thousand") {
                total += (group || 1) * 1000;
                group = 0;
            } else group += numberWords[word] || 0;
        }
        return total + group;
    }

    function parseAmount(text) {
        const words = "zero|oh|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|thousand|and";
        const number = `(?:\\d[\\d,]*(?:\\.\\d{1,2})?|(?:${words})(?:\\s+(?:${words}))*)`;
        const money = new RegExp(`(?:₹\\s*|\\b(?:rs\\.?|inr|rupees?)\\s*)(${number})|(${number})\\s*(?:rupees?|rs\\.?|inr)\\b`, "i");
        const match = text.match(money);
        if (match) {
            const raw = match[1] || match[2];
            const value = /\d/.test(raw) ? Number(raw.replace(/,/g, "")) : parseNumberWords(raw);
            if (Number.isFinite(value) && value > 0) return { value, text: match[0] };
        }
        const fallback = text.match(/\b\d[\d,]*(?:\.\d{1,2})?\b/);
        if (!fallback) return null;
        const value = Number(fallback[0].replace(/,/g, ""));
        return Number.isFinite(value) && value > 0 ? { value, text: fallback[0] } : null;
    }

    function parseCategory(text) {
        const options = Array.from(categoryField.options).filter(option => option.value);
        for (const option of options.sort((a, b) => b.value.length - a.value.length)) {
            const escaped = option.value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
            const match = text.match(new RegExp(`\\b${escaped}\\b`, "i"));
            if (match) return { value: option.value, text: match[0] };
        }
        return null;
    }

    function todayAsInputValue(offsetDays = 0) {
        const date = new Date();
        date.setDate(date.getDate() + offsetDays);
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    }

    function toInputDate(year, month, day) {
        const date = new Date(Date.UTC(year, month - 1, day));
        if (year < 1000 || date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
        return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    }

    function parseExplicitDate(text) {
        const iso = text.match(/\b(\d{4})-(\d{1,2})-(\d{1,2})\b/);
        if (iso) {
            const value = toInputDate(Number(iso[1]), Number(iso[2]), Number(iso[3]));
            if (value) return { value, text: iso[0] };
        }

        // Finora's date display follows the user's India locale: DD/MM/YYYY.
        const local = text.match(/\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})\b/);
        if (local) {
            const value = toInputDate(Number(local[3]), Number(local[2]), Number(local[1]));
            if (value) return { value, text: local[0] };
        }
        return null;
    }

    function fillExpense() {
        const original = input.value.trim();
        if (!original) {
            setStatus("Type an expense sentence first, for example: “Milk, 36 rupees, food, today.”", true);
            input.focus();
            return;
        }

        const amount = parseAmount(original);
        const category = parseCategory(original);
        const explicitDate = parseExplicitDate(original);
        const yesterday = /\byesterday\b/i.test(original);
        const today = /\btoday\b/i.test(original);
        let description = original;

        if (amount) {
            description = description.replace(amount.text, " ");
            amountField.value = amount.value.toFixed(2);
            amountField.dispatchEvent(new Event("input", { bubbles: true }));
        }
        if (category) {
            description = description.replace(new RegExp(category.text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i"), " ");
            categoryField.value = category.value;
            categoryField.dispatchEvent(new Event("change", { bubbles: true }));
        }
        if (explicitDate) {
            description = description.replace(explicitDate.text, " ");
            dateField.value = explicitDate.value;
            dateField.dispatchEvent(new Event("change", { bubbles: true }));
        } else if (today || yesterday) {
            description = description.replace(/\b(?:today|yesterday)\b/gi, " ");
            dateField.value = todayAsInputValue(yesterday ? -1 : 0);
            dateField.dispatchEvent(new Event("change", { bubbles: true }));
        }

        const startsWithAction = /^\s*(?:(?:i|we)\s+)?(?:spent|spend|paid|pay|bought|purchased|logged|recorded)\b/i.test(original);
        description = description
            .replace(/^\s*(?:(?:i|we)\s+)?(?:spent|spend|paid|pay|bought|purchased|logged|recorded)\b\s*/i, " ")
            .replace(/\b(?:today|yesterday)\b/gi, " ")
            .replace(/^\s*(?:expense|purchase)\s+(?:for|of)\b/i, " ")
            .replace(/^\s*(?:on|for|at|in)\b\s*/i, startsWithAction ? " " : "")
            .replace(/\s+\b(?:for|on|at|in|and)\s*$/i, " ")
            .replace(/[,.!?;:]+/g, " ")
            .replace(/\s+/g, " ")
            .trim();

        if (!description && category) description = `${category.value} expense`;

        if (description) {
            descriptionField.value = description;
            descriptionField.dispatchEvent(new Event("input", { bubbles: true }));
        }

        const missing = [];
        if (!description) missing.push("description");
        if (!amount) missing.push("amount");
        if (!category) missing.push("category");
        const message = missing.length
            ? `Filled what I could. Please check ${missing.join(", ")} in the form.`
            : "Expense details filled. Review them in the form, then save when ready.";
        setStatus(message, missing.length > 0);
        if (!description) descriptionField.focus();
        else if (!amount) amountField.focus();
    }

    fillButton.addEventListener("click", fillExpense);
    input.addEventListener("keydown", event => {
        if ((event.metaKey || event.ctrlKey) && event.key === "Enter") fillExpense();
    });
})();
