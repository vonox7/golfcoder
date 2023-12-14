async function submitForm(event) {
    event.preventDefault();

    const originalButtonText = event.target.value;
    const form = event.target.closest("form");
    let resetButtonTextSeconds = 2;
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
            if (responseData.alertHtml) {
                document.getElementById("error-dialog-body").innerHTML = responseData.alertHtml;
                document.getElementById("error-dialog").showModal();
            }
            resetButtonTextSeconds = responseData.resetButtonTextSeconds;
            // responseData.changeInput is a map<String, String> of input name to new value. Change the form input values:
            for (const [name, value] of Object.entries(responseData.changeInput)) {
                form.querySelector(`[name="${name}"]`).value = value;
            }
            // responseData.setInnerHtml is a map<String, String> of element id to new innerHTML. Change the innerHTML:
            for (const [id, innerHtml] of Object.entries(responseData.setInnerHtml)) {
                document.getElementById(id).innerHTML = innerHtml;
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
    if (resetButtonTextSeconds) {
        setTimeout(() => {
            event.target.value = originalButtonText;
        }, resetButtonTextSeconds * 1000);
    }
}

function resetSubmitForm(event) {
    const form = event.target.closest("form");
    form.querySelector("[name=onlyTokenize]").value = "on";
    form.querySelector("[name=submitButton]").value = "Calculate tokens";
}