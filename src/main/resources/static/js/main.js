async function submitForm(event) {
    event.preventDefault();

    const originalButtonText = event.target.value;
    const form = event.target.closest("form");
    event.target.value += "...";

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
        }

    } catch (error) {
        console.error(error);
        event.target.value = "Error";
    }

    setTimeout(() => {
        event.target.value = originalButtonText;
    }, 2000);
}