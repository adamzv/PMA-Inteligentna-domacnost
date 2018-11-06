document.addEventListener("DOMContentLoaded", () => {
    document.querySelector("#notifikacia").onsubmit = () => {
        const request = new XMLHttpRequest();
        const sprava = document.querySelector("#text").value;
        const heslo = document.querySelector("#heslo").value;
        request.open("POST", "/api/notifikacia");

        request.onload = () => {
            let response = JSON.parse(request.responseText);
            if (parseInt(response, 10) === 202) {
                document.querySelector("#vysledok").innerHTML = "Správa bola odoslaná.";
            } else {
                document.querySelector("#vysledok").innerHTML = "Niekde nastala chyba.";
            }
        };

        const data = new FormData();
        data.append("sprava", sprava);
        data.append("heslo", heslo);

        request.send(data);
        return false;
    };
    document.querySelector("#alarm").onsubmit = () => {
        const request = new XMLHttpRequest();
        const select = document.querySelector("#zapnut");

        request.open("POST", "/api/alarm");

        request.onload = () => {
        };

        request.setRequestHeader("Content-Type", "application/json");
        request.send(JSON.stringify({"senzor": "pir", "status": select.value}));
        return false;
    };
    document.querySelector("#dvere").onsubmit = () => {
        const request = new XMLHttpRequest();
        const select = document.querySelector("#otvorit");
        const sprava = '{"senzor": "servo", "hodnota": "dvere", "status": "' + select.value + '"}';

        request.open("POST", "/api/dvere");

        request.onload = () => {
        };

        request.setRequestHeader("Content-Type", "application/json");
        request.send(sprava);
        return false;
    };
});;