```javascript
document.addEventListener("DOMContentLoaded", function () {
    // 1. Animate dashboard numbers
    const counters = document.querySelectorAll("[data-count]");

    counters.forEach(function (counter) {
        const target = Number(counter.getAttribute("data-count"));
        const duration = 1000;
        const startTime = performance.now();

        function updateCounter(currentTime) {
            const progress = Math.min(
                (currentTime - startTime) / duration,
                1
            );

            counter.textContent = Math.floor(target * progress);

            if (progress < 1) {
                requestAnimationFrame(updateCounter);
            } else {
                counter.textContent = target;
            }
        }

        requestAnimationFrame(updateCounter);
    });

    // 2. Search the visible ticket table
    const searchInput = document.getElementById("globalSearch");
    const ticketRows = document.querySelectorAll("#ticketsTable tbody tr");

    if (searchInput) {
        searchInput.addEventListener("input", function () {
            const searchText = searchInput.value.toLowerCase().trim();

            ticketRows.forEach(function (row) {
                const rowText = row.textContent.toLowerCase();
                row.style.display = rowText.includes(searchText) ? "" : "none";
            });
        });
    }

    // 3. Toggle sidebar on smaller screens
    const menuToggle = document.getElementById("menuToggle");
    const sidebar = document.getElementById("sidebar");

    if (menuToggle && sidebar) {
        menuToggle.addEventListener("click", function () {
            sidebar.classList.toggle("sidebar-open");
        });
    }

    // Close mobile sidebar when a navigation link is selected
    document.querySelectorAll(".sidebar .nav-link").forEach(function (link) {
        link.addEventListener("click", function () {
            if (sidebar) {
                sidebar.classList.remove("sidebar-open");
            }
        });
    });

    // 4. Toggle dark mode
    const themeToggle = document.getElementById("themeToggle");

    if (themeToggle) {
        themeToggle.addEventListener("click", function () {
            document.body.classList.toggle("dark-mode");

            const icon = themeToggle.querySelector("i");

            if (icon) {
                icon.classList.toggle("fa-moon");
                icon.classList.toggle("fa-sun");
            }
        });
    }

    // 5. Add a small entrance animation to dashboard cards
    const cards = document.querySelectorAll(
        ".metric-card, .panel"
    );

    cards.forEach(function (card, index) {
        card.style.animation = "fadeUp 0.5s ease both";
        card.style.animationDelay = (index * 70) + "ms";
    });

    // 6. Highlight the current page in the sidebar
    const currentPage = window.location.pathname.split("/").pop()
        || "index.html";

    document.querySelectorAll(".sidebar .nav-link").forEach(function (link) {
        const linkPage = link.getAttribute("href");

        if (linkPage === currentPage) {
            link.classList.add("active");
        } else {
            link.classList.remove("active");
        }
    });
});
```