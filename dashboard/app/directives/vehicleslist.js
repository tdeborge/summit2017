'use strict';


angular.module('app').directive('vehiclesList', function () {


	return {
		restrict: 'E',
		replace: true,
		templateUrl: 'partials/vehicleslist.html',
		controller: 'VehiclesListController'
	}
});
