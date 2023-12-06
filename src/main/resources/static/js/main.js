async function submitForm(event) {
    event.preventDefault();

    const originalButtonText = event.target.value;
    const form = event.target.closest("form");
    event.target.value += "...";
    event.target.disabled = true;

    try {
        const response = await fetch(form.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            body: JSON.stringify(Object.fromEntries(new FormData(form).entries()))
        });

        if (!response.ok) {
            console.error(response.status, await response.text());
            event.target.value = "Error - " + response.status;
        } else {
            const responseData = await response.json();
            if (responseData.buttonText) {
                event.target.value = responseData.buttonText;
            }
            if (responseData.reloadSite) {
                window.location.reload();
            }
        }

    } catch (error) {
        console.error(error);
        event.target.value = "Error";
    }

    event.target.disabled = false;
    setTimeout(() => {
        event.target.value = originalButtonText;
    }, 2000);
}