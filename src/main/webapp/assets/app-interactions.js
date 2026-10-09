(() => {
    document.querySelectorAll("[data-toast]").forEach(toast => {
        const close = toast.querySelector("[data-toast-close]");
        const dismiss = () => {
            toast.classList.add("notice-leaving");
            window.setTimeout(() => toast.remove(), 220);
        };
        close?.addEventListener("click", dismiss);
        if (toast.dataset.toastType === "success") window.setTimeout(dismiss, 6500);
    });

    document.querySelectorAll("form[data-confirm]").forEach(form => {
        form.addEventListener("submit", event => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
                return;
            }
            const button = event.submitter;
            if (button) {
                button.disabled = true;
                button.textContent = "Deleting…";
            }
        });
    });

    const currentPath = window.location.pathname.replace(/\/$/, "");
    document.querySelectorAll(".main-nav a").forEach(link => {
        const linkPath = new URL(link.href, window.location.href).pathname.replace(/\/$/, "");
        if (linkPath === currentPath) {
            link.classList.add("is-current");
            link.setAttribute("aria-current", "page");
        }
    });

    const search = document.querySelector("[data-expense-search]");
    const expenseRows = Array.from(document.querySelectorAll("[data-expense-row]"));
    if (search && expenseRows.length) {
        const count = document.querySelector("[data-expense-count]");
        const noResults = document.querySelector("[data-expense-no-results]");
        const updateSearch = () => {
            const query = search.value.trim().toLocaleLowerCase();
            let shown = 0;
            expenseRows.forEach(row => {
                const matches = row.dataset.expenseSearch.toLocaleLowerCase().includes(query);
                row.hidden = !matches;
                if (matches) shown++;
            });
            if (count) count.textContent = `${shown} ${shown === 1 ? "expense" : "expenses"} shown`;
            if (noResults) noResults.hidden = shown !== 0;
        };
        search.addEventListener("input", updateSearch);
        updateSearch();
    }
})();
