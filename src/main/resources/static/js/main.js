async function submitForm(event) {
    event.preventDefault();

    const originalButtonText = event.target.value;
    const form = event.target.closest("form");
    let resetButtonTextSeconds = 2;
    event.target.disabled = true;

    // UX: Change button text to something meaningful to indicate progress
    let buttonTextChangeInterval = undefined;
    const submitState = form.querySelector("[name=submitState]")?.value;
    if (submitState === "ONLY_TOKENIZE") {
        event.target.value = "Tokenizing...";
    } else if (submitState === "SUBMIT_NOW") {
        event.target.value = "Tokenizing...";
        const buttonStrings = ["Compiling...", "Executing...", "Validating stdout..."];
        let buttonStringPosition = 0;
        buttonTextChangeInterval = setInterval(() => {
            event.target.value = buttonStrings[buttonStringPosition];
            buttonStringPosition++;
            if (buttonStringPosition === buttonStrings.length) {
                clearInterval(buttonTextChangeInterval);
            }
        }, 500);
    } else {
        event.target.value += "...";
    }

    // Send the form data to the server
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
                clearInterval(buttonTextChangeInterval);
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
            if (responseData.redirect) {
                window.location.href = responseData.redirect;
            }
        }

    } catch (error) {
        console.error(error);
        event.target.value = "Error";
    }

    // Reset button
    event.target.disabled = false;
    if (resetButtonTextSeconds) {
        setTimeout(() => {
            clearInterval(buttonTextChangeInterval);
            event.target.value = originalButtonText;
        }, resetButtonTextSeconds * 1000);
    }
}

function resetSubmitForm(event) {
    const form = event.target.closest("form");
    form.querySelector("[name=submitState]").value = "ONLY_TOKENIZE";
    form.querySelector("[name=submitButton]").value = "Calculate tokens";
}

function fillTemplate(event) {
    const form = event.target.closest("form");
    const value = event.target.value;
    form.querySelector("[name=code]").value = event.target.querySelector(`[value="${value}"]`).dataset.template;
}