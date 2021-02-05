"use strict";

const app = angular.module("demoAppModule", ["ui.bootstrap"]);

app.config([
  "$qProvider",
  function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
  },
]);

app.controller(
  "DemoAppController",
  function ($http, $location, $uibModal, $scope) {
    const demoApp = this;

    //Werte, um die Anzeigen zu steuern
    demoApp.thisNodeFarmer = false;
    demoApp.thisNodeCB = false;
    demoApp.thisNodeTrader = false;
    demoApp.showGrapes = true;
    demoApp.showCerts = false;
    demoApp.showRequest = false;
    demoApp.showPeers = false;
    demoApp.showTransaction = false;

    //Stellt eine Anfrage an den Server und erhält die GrapeStates zurück
    demoApp.getGrapeStates = function () {
      $http.get("/grapestates").then(function (response) {
        demoApp.myGrapeStates = response.data.grapestates;
        console.log("/getGrapeStates");
      });
    };

    //Stellt eine Anfrage an den Server und erhält die sonstigen Netzwerkteilnehmer zurück
    demoApp.getPeers = function () {
      $http.get("/peers").then(function (response) {
        demoApp.peers = response.data.peers;
        console.log("/getPeers");
      });
    };

    //Stellt eine Anfrage an den Server und erhält die CertStates zurück
    demoApp.getCertStates = function () {
      $http.get("/certstates").then(function (response) {
        demoApp.myCertStates = response.data.certstates;
        console.log("/getCertStates");
      });
    };

    //Stellt eine Anfrage an den Server und erhält die RequestStates zurück
    demoApp.getRequestStates = function () {
      $http.get("/reqstates").then(function (response) {
        demoApp.myRequestStates = response.data.req;
        console.log("/getRequestStates");
      });
    };

    //Stellt eine Anfrage an den Server und erhält die eigene Identität zurück und bringt diese in die passende Form
    demoApp.getMe = function () {
      $http.get("/me").then(function (response) {
        demoApp.thisNode =
          "O=" +
          response.data.me.organisation +
          ", L=" +
          response.data.me.locality +
          ", C=" +
          response.data.me.country;
        console.log("/me " + demoApp.thisNode.toString());
        console.log("myCommonname " + response.data.me.commonName.toString());
        if (response.data.me.commonName.toString().includes("Farmer")) {
          demoApp.thisNodeFarmer = true;
        }
        if (response.data.me.commonName.toString().includes("ZS")) {
          demoApp.thisNodeCB = true;
          demoApp.changeView("certs");
        }
        if (response.data.me.commonName.toString().includes("Trader")) {
          demoApp.thisNodeTrader = true;
        }
        if (response.data.me.commonName.toString().includes("Notary")) {
          demoApp.thisNodeNotary = true;
        }
      });
    };

    //Stellt eine Anfrage an den Server und erhält die TransactionStates zurück
    demoApp.getTx = function () {
      $http.get("/gettx").then(function (response) {
        demoApp.transactions = response.data.tx;
        console.log("/gettx");
      });
    };

    //Ruft alle Methoden zum Sammeln der States auf
    demoApp.getUpgrades = function () {
      demoApp.getPeers();
      demoApp.getGrapeStates();
      demoApp.getCertStates();
      demoApp.getMe();
      demoApp.getRequestStates();
      demoApp.getTx();
    };

    demoApp.getUpgrades();

    //Ändert die Ansicht je nachdem welcher Wert mitgegeben wurde
    demoApp.changeView = ($value) => {
      demoApp.showGrapes = false;
      demoApp.showCerts = false;
      demoApp.showRequest = false;
      demoApp.showPeers = false;
      demoApp.showTransaction = false;
      demoApp.showTXValues = false;
      switch ($value) {
        case "grapes":
          demoApp.showGrapes = true;
          break;
        case "certs":
          demoApp.showCerts = true;
          break;
        case "transactions":
          demoApp.getTx();
          demoApp.showTransaction = true;
          break;
        case "peers":
          demoApp.showPeers = true;
          break;
        case "data":
          demoApp.getFiles();
          demoApp.showData = true;
          break;
        default:
          demoApp.showGrapes = true;
      }
    };

    demoApp.showTXValues = false;   //Steuert die Ansicht der Transaktionen für eine einzelne Kiste
    demoApp.currentStateIDShown = "";   //Der Name, der angezeigten Kiste
    demoApp.transactionShort = [];    //Ein Array, das alle Transaktionen der entsprechenden Kiste enthält
    demoApp.txIDsToShow = [];   //Ein Array, das die IDs der Transaktionen enthält
    demoApp.txToUse = {};   //Ein Wert, der angibt welche Transaktion betrachtet wird
    var spanText = document.getElementsByClassName("toSearchInSpan");

    //Steuert die Anzeige der Transaktionen einer Kiste und berechnet sie, falls nötig, neu
    demoApp.showTXValuesShort = ($stateID) => {
      if (demoApp.showTXValues && demoApp.currentStateIDShown != $stateID) {
        demoApp.currentStateIDShown = $stateID;
        demoApp.getTrancactionsShort(demoApp.currentStateIDShown);
      } else {
        demoApp.currentStateIDShown = $stateID;
        if (demoApp.showTXValues) {
          demoApp.showTXValues = false;
        } else {
          demoApp.showTXValues = true;
          demoApp.getTrancactionsShort(demoApp.currentStateIDShown);
        }
      }
    };

    //Füllt das txIDsToShow Array
    demoApp.getTrancactionsShort = ($ID) => {
      demoApp.transactionShort = [];

      var transTMP = [...demoApp.transactions];
      transTMP = transTMP.reverse();

      for (let tx of transTMP) {
        if (tx.output.toString().includes($ID)) {
          demoApp.txIDsToShow.push(tx.id);
          demoApp.txToUse = tx;
          break;
        }
      }

      demoApp.getInputsOfTX(demoApp.txToUse.id);

      for (let id of demoApp.txIDsToShow) {
        for (let tx of demoApp.transactions) {
          if (tx.id == id) {
            demoApp.transactionShort.push(tx);
            break;
          }
        }
      }

      demoApp.txIDsToShow = [];
    };

    //Prüft rekursiv, ob es noch Input Transaktionen gibt, die noch nicht im txIDsToShow Array stehen
    demoApp.getInputsOfTX = (tx) => {
      var tmpTX = demoApp.transactions.find((element) => element.id == tx);

      if (tmpTX.input == "Keine") {
      } else {
        demoApp.txIDsToShow.push(tmpTX.id);
        var tmp = tmpTX.input.split(", ");
        demoApp.txIDsToShow = demoApp.txIDsToShow.concat(tmp);
        demoApp.txIDsToShow = demoApp.txIDsToShow.filter(
          (value, index) => demoApp.txIDsToShow.indexOf(value) === index
        );
        tmp.forEach((it) => demoApp.getInputsOfTX(it));
      }
    };

    //Zeigt den Alert an, wenn ein Inhalt zu groß für den eigentlichen Bildschirm wäre
    demoApp.alert = ($alertMessage) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "txMessageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => $alertMessage, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    //Steuert den Kisten erstellen Dialog
    demoApp.openCreate = () => {
      const openCreateInstance = $uibModal.open({
        templateUrl: "openCreate.html",
        controller: "CreateGrapesCtrl",
        controllerAs: "openCreateInstance",
        resolve: {
          demoApp: () => demoApp,
          peers: () => demoApp.peers,
          myGrapeStates: () => demoApp.myGrapeStates,
          grapeDes: () => demoApp.grapeDes,
        },
      });
      openCreateInstance.result.then(
        () => {},
        () => {}
      );
    };

    //Steuert den Kisten handeln Dialog
    demoApp.openTrade = ($state) => {
      const openTradeInstance = $uibModal.open({
        templateUrl: "openTrade.html",
        controller: "TradeGrapeCtrl",
        controllerAs: "openTradeInstance",
        resolve: {
          demoApp: () => demoApp,
          peers: () => demoApp.peers,
          myGrapeState: () => $state,
        },
      });
      openTradeInstance.result.then(
        () => {},
        () => {}
      );
    };

    //Steuert den Zertifikat verteilen Dialog
    demoApp.openCert = ($state, $description, $node) => {
      console.log("openCert " + $state + $description + $node);
      const openCertInstance = $uibModal.open({
        templateUrl: "openCert.html",
        controller: "CertCtrl",
        controllerAs: "openCertInstance",
        resolve: {
          demoApp: () => demoApp,
          myRequestState: () => $state,
          description: () => $description,
          node: () => $node,
        },
      });
      openCertInstance.result.then(
        () => {},
        () => {}
      );
    };

    //Steuert den Zertifikat anfragen Dialog
    demoApp.openReq = () => {
      const openReqInstance = $uibModal.open({
        templateUrl: "openReq.html",
        controller: "ReqCtrl",
        controllerAs: "openReqInstance",
        resolve: {
          demoApp: () => demoApp,
          peers: () => demoApp.peers,
          grapeDes: () => demoApp.grapeDes,
        },
      });
      openReqInstance.result.then(
        () => {},
        () => {}
      );
    };

    //Steuert den Kisten zertifizieren Dialog
    demoApp.openCertify = ($stateID, $stateDescription) => {
      const openCertifyInstance = $uibModal.open({
        templateUrl: "openCertify.html",
        controller: "CertifyCtrl",
        controllerAs: "openCertifyInstance",
        resolve: {
          demoApp: () => demoApp,
          peers: () => demoApp.peers,
          grapeDes: () => demoApp.grapeDes,
          myCertStates: () => demoApp.myCertStates,
          myGrapeStateID: () => $stateID,
          myGrapeStateDescription: () => $stateDescription,
        },
      });
      openCertifyInstance.result.then(
        () => {},
        () => {}
      );
    };
  }
);

//Controller zum Kiste erstellen
app.controller(
  "CreateGrapesCtrl",
  function (
    $http,
    $location,
    $uibModalInstance,
    $uibModal,
    demoApp,
    peers,
    myGrapeStates,
    grapeDes
  ) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.myGrapeStates = myGrapeStates;
    modalInstance.grapeDes = grapeDes;
    modalInstance.form = {};
    modalInstance.formError = false;

    modalInstance.create = function createGrapeState() {

      var counter = document.getElementById("valuecounter").value;
      var result = "";
      for (var i = 1; i <= counter; i++) {
        if (document.getElementById("title" + i) === null) {
          continue;
        }
        result +=
          document.getElementById("title" + i).textContent +
          document.getElementById("check" + i).value +
          "(,)";
        console.log(
          document.getElementById("title" + i).textContent +
            document.getElementById("check" + i).value
        );
      }
      result = result.substring(0, result.length - 3);
      var resultCheck = (result.slice(-1) == ":" || result.includes(":(,)"));
      console.log("erstes IF: " + result.slice(-1) == ":" + "zweites IF: " + result.includes(":(,)") + "gesamt: " + resultCheck);

      if (!modalInstance.form.weight || !modalInstance.form.number || !modalInstance.form.desc || resultCheck) {
        modalInstance.formError = true;
      } else {
        modalInstance.formError = false;
        $uibModalInstance.close();

        let CREATE_GRAPES_PATH = "/produceGrape";

        let createGrapeData = $.param({
          weight: modalInstance.form.weight,
          number: modalInstance.form.number,
          desc: modalInstance.form.desc,
          values: result,
        });
        let createGrapeHeaders = {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        };

        $http
          .post(CREATE_GRAPES_PATH, createGrapeData, createGrapeHeaders)
          .then(modalInstance.displayMessage, modalInstance.displayMessage);
      }
    };

    modalInstance.displayMessage = (message) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "messageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => message, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    modalInstance.cancel = () => $uibModalInstance.dismiss();
  }
);

//Controller zum Kiste handeln
app.controller(
  "TradeGrapeCtrl",
  function (
    $http,
    $location,
    $uibModalInstance,
    $uibModal,
    demoApp,
    peers,
    myGrapeState
  ) {
    const modalInstance = this;
    modalInstance.peers = peers.filter((item) => item.commonName == "Trader");
    modalInstance.myState = myGrapeState;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.create = function tradeGrapeState() {
      if (!modalInstance.form.newOwner) {
        modalInstance.formError = true;
      } else {
        modalInstance.formError = false;
        $uibModalInstance.close();

        let TRADE_GRAPE_PATH = "/tradeGrape";

        let tradeGrapeData = $.param({
          newOwner: modalInstance.form.newOwner.organisation,
          grapeId: modalInstance.myState,
        });
        console.log("grapeID " + modalInstance.myState);
        console.log("newOwner " + modalInstance.form.newOwner.organisation);

        let tradeGrapeHeaders = {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        };

        $http
          .post(TRADE_GRAPE_PATH, tradeGrapeData, tradeGrapeHeaders)
          .then(modalInstance.displayMessage, modalInstance.displayMessage);
      }
    };

    modalInstance.displayMessage = (message) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "messageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => message, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    modalInstance.cancel = () => $uibModalInstance.dismiss();
  }
);

//Controller zum Zertifikat verteilen
app.controller(
  "CertCtrl",
  function (
    $http,
    $location,
    $uibModalInstance,
    $uibModal,
    demoApp,
    description,
    node,
    myRequestState
  ) {
    const modalInstance = this;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.cert = function createCertState() {
      if (!modalInstance.form.period) {
        modalInstance.formError = true;
      } else {
        modalInstance.formError = false;
        $uibModalInstance.close();

        let CREATE_CERT_PATH = "/createCert";
        console.log("reqId " + myRequestState);
        console.log("node " + node);
        console.log("description " + description);
        let createCertData = $.param({
          owningParty: node,
          description: description,
          period: modalInstance.form.period,
          requestId: myRequestState,
          number: "1",
        });
        let createCertHeaders = {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        };

        $http
          .post(CREATE_CERT_PATH, createCertData, createCertHeaders)
          .then(modalInstance.displayMessage, modalInstance.displayMessage);
      }
    };

    modalInstance.displayMessage = (message) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "messageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => message, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    modalInstance.cancel = () => $uibModalInstance.dismiss();
  }
);

//Controller zum Zertifizieren der Kiste
app.controller(
  "CertifyCtrl",
  function (
    $http,
    $location,
    $uibModalInstance,
    $uibModal,
    $scope,
    demoApp,
    peers,
    grapeDes,
    myCertStates,
    myGrapeStateID,
    myGrapeStateDescription
  ) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.grapeDes = grapeDes;
    modalInstance.myCertStates = myCertStates;
    modalInstance.myGrapeStateID = myGrapeStateID;
    modalInstance.myGrapeStateDescription = myGrapeStateDescription;
    modalInstance.form = {};
    modalInstance.formError = false;

    $scope.cert = false;
    $scope.certID = "";

    modalInstance.myCertStates.forEach(function (item) {
      if (item.Beschreibung.includes(modalInstance.myGrapeStateDescription)) {
        $scope.certID = item.ID;
        $scope.cert = true;
        console.log("CertID " + $scope.certID);
        console.log("cert " + $scope.cert);
      }
    });

    modalInstance.certify = function createCertState() {
      if (!$scope.cert) {
        modalInstance.formError = true;
      } else {
        modalInstance.formError = false;
        $uibModalInstance.close();
        let CREATE_CERT_PATH = "/certifyGrape";

        let createCertData = $.param({
          grapeId: modalInstance.myGrapeStateID,
          certId: $scope.certID,
          description: true,
        });

        let createCertHeaders = {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        };

        $http
          .post(CREATE_CERT_PATH, createCertData, createCertHeaders)
          .then(modalInstance.displayMessage, modalInstance.displayMessage);
      }
    };

    modalInstance.displayMessage = (message) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "messageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => message, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    modalInstance.cancel = () => $uibModalInstance.dismiss();
  }
);

//Controller zum Zertifikat anfragen
app.controller(
  "ReqCtrl",
  function (
    $http,
    $location,
    $uibModalInstance,
    $uibModal,
    demoApp,
    peers,
    grapeDes
  ) {
    const modalInstance = this;
    modalInstance.peers = peers.filter((item) => item.commonName == "ZS");
    modalInstance.grapeDes = grapeDes;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.req = function creataRequestState() {
      if (!modalInstance.form.certificationBody || !modalInstance.form.description || !modalInstance.form.number) {
        modalInstance.formError = true;
      } else {
        modalInstance.formError = false;
        $uibModalInstance.close();

        let CREATE_REQ_PATH = "/createReq";

        let createReqData = $.param({
          description: modalInstance.form.description,
          certificationBody: modalInstance.form.certificationBody.organisation,
          number: modalInstance.form.number,
        });

        let createReqHeaders = {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        };

        $http
          .post(CREATE_REQ_PATH, createReqData, createReqHeaders)
          .then(modalInstance.displayMessage, modalInstance.displayMessage);
      }
    };

    modalInstance.displayMessage = (message) => {
      const modalInstanceTwo = $uibModal.open({
        templateUrl: "messageContent.html",
        controller: "messageCtrl",
        controllerAs: "modalInstanceTwo",
        resolve: { message: () => message, demoApp: () => demoApp },
      });

      modalInstanceTwo.result.then(
        () => {},
        () => {}
      );
    };

    modalInstance.cancel = () => $uibModalInstance.dismiss();
  }
);

//Controller zum Anzeigen der Flow Antworten als Alert
app.controller("messageCtrl", function ($uibModalInstance, message, demoApp) {
  const modalInstanceTwo = this;
  modalInstanceTwo.message = message.data.message;
  demoApp.getUpgrades();
});

//Controller zum Anzeigen der zu großen Nachrichten als Alert
app.controller("txMessageCtrl", function ($uibModalInstance, message) {
  const modalInstanceTwo = this;
  modalInstanceTwo.message = message;
});
