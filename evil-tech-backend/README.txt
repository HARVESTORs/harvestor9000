EvilTech Storefront
===================

Copy:
  src/main/resources/static/index.html
into the same path in the Spring Boot project.

Run the backend, then open:
  http://127.0.0.1:8080/index.html

For LAN access, bind the backend to 0.0.0.0 and open:
  http://YOUR-LAN-IP:8080/index.html

The UI auto-creates a simulated shopper session and uses the six valid backend SKU IDs.
Three additional parody products are displayed as unavailable so they never send invalid product IDs.
