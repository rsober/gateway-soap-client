package com.anypresence.wsinvoker;

public class ChargepointExample {
	/*public static void main(String[] args) {
		Chargepointservices_Service svcImpl = new Chargepointservices_Service();
	    Chargepointservices svc = svcImpl.getChargepointservicesSOAP();

	    Binding binding = ((BindingProvider) svc).getBinding();
	    List<Handler> handlerList = binding.getHandlerChain();
	    if (handlerList == null) {
	      handlerList = new ArrayList<Handler>();
	    }

	    handlerList.add(new SecurityHandler("e39e34c98def1db64229f0554306133b53fd057661b271409090934", "217eb8c9c92b91c52f25c9354cc773e2"));
	    binding.setHandlerChain(handlerList);

	    StationSearchRequestExtended searchQuery = new StationSearchRequestExtended();
	    searchQuery.setOrgID("1:ORG08313");
	    Holder<String> responseCode = new Holder<String>();
	    Holder<String> responseText = new Holder<String>();
	    Holder<List<StationDataExtended>> stationData = new Holder<List<StationDataExtended>>(new ArrayList<StationDataExtended>());
	    try {
	      svc.getStations(searchQuery, responseCode, responseText, stationData);
	      System.out.println("Response Code: " + responseCode.value);
	      System.out.println("Response Text: " + responseText.value);
	      System.out.println("Station Data: " + stationData.value);
	    } catch (javax.xml.ws.WebServiceException e) {
	       Throwable t = e.getCause();
	       if (t != null && t instanceof XMLStreamException) {
	         XMLStreamException xse = (XMLStreamException)t;
	         Location loc = xse.getLocation();
	         xse.printStackTrace();
	         System.out.println("LOC " + loc);
	         System.out.println("NESTED " + xse.getNestedException());
	       } else {
	         e.printStackTrace();
	       }
	     }
	}*/
}
