var polyline;

var markerCount = 0;
var maxMarkers = 10;

var pathMarker1;
var pathMarker2;
var pathMarker3;

var markerGroup = L.featureGroup().addTo(map);

var targetIndex;
var linecolor = '#3030DD';
var sampleMessage;

map.locate({setView: true}).on('locationfound', function (e) {
    var marker = new L.marker(e.latlng, {draggable: true});
    map.addLayer(marker);
});


function CalculateSamplePath(e) {

    var startNodeCoords = [pathMarker1.getLatLng().lat, pathMarker1.getLatLng().lng];
    var targetNodeCoords = [pathMarker2.getLatLng().lat, pathMarker2.getLatLng().lng];

    var urlString = "/shortestPathFromTo/" + startNodeCoords + "/" + targetNodeCoords;
    $.ajax({
        type: "GET",
        url: urlString,
        timeout: 20000,
        success: function (response) {
            var latlngs = response.split(",").map(function (e) {
                return e.split("_").map(Number);
            });
            if (polyline != undefined) {
                map.removeLayer(polyline)
            }
            polyline = L.polyline(latlngs, {
                color: linecolor
            }).addTo(map);
            map.fitBounds(polyline.getBounds());

        },
        error: function () {
            sampleMessage = "target Index failed"
        }
    });

}

function GetPOIsInRangeFunction(e) {
    console.log(" GET POIs Called");
}

function generateRoundTripBetweenMarkers(e) {
    var markerlats = [
        pathMarker1.getLatLng().lat,
        pathMarker2.getLatLng().lat,
        pathMarker3.getLatLng().lat,
    ];

    var markerlngs = [
        pathMarker1.getLatLng().lng,
        pathMarker2.getLatLng().lng,
        pathMarker3.getLatLng().lng,
    ];

    var urlString = "/generateRoundtrip/" + markerlats + "/" + markerlngs;
    $.ajax({
        type: "GET",
        url: urlString,
        timeout: 20000,
        success: function (response) {
            var latlngs = response.split(",").map(function (e) {
                return e.split("_").map(Number);
            });
            if (polyline != undefined) {
                map.removeLayer(polyline)
            }
            polyline = L.polyline(latlngs, {
                color: linecolor
            }).addTo(map);
            map.fitBounds(polyline.getBounds());

        },
        error: function () {
            sampleMessage = "target Index failed";
            console.log(sampleMessage)
        }
    });
}

/* CONTEXT MENU */
/* VARIABLES */
var menu = document.querySelector("#context-menu");
var menuState = 0;
var active = "context-menu--active";

/* CONTEXT MENU FUNCTION */
(function () {

    "use strict";

    var taskItems = document.querySelectorAll(".task");

    for (var i = 0, len = taskItems.length; i < len; i++) {
        var taskItem = taskItems[i];
        contextMenuListener(taskItem);
    }

    function contextMenuListener(el) {
        el.addEventListener("contextmenu", function (e) {
            e.preventDefault();
            toggleMenuOn();
        });
    }

    function toggleMenuOn() {
        if (menuState !== 1) {
            menuState = 1;
            menu.classList.add(active);
        }
    }

})();

map.on('click', function (e) {

    if (typeof (pathMarker1) === 'undefined') {
        map.stopLocate();
        pathMarker1 = new L.marker(e.latlng, {draggable: true});
        pathMarker1.addTo(map);

    } else if (typeof(pathMarker2) === 'undefined') {
        pathMarker2 = new L.marker(e.latlng, {draggable: true});
        pathMarker2.addTo(map);
    } else if (typeof(pathMarker3) === 'undefined') {
        pathMarker3 = new L.marker(e.latlng, {draggable: true});
        pathMarker3.addTo(map);
    } else {
        pathMarker3 = new L.marker(e.latlng, {draggable: true});
    }
});