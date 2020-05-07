# dal adapter for MiddleAtlantic PDU with RackLink™

Since RackLink™ devices provide individual IP outlet control - this is the main goal 
and motivation behind creating an adapter for it - to have an access to this functionality 
via Symphony.

A communication is done via REST API, which allows to get such information about the 
device as:
- Active Power
- Power Line Frequency
- Voltage
- Outlet current

As well as it allows to control each outlet separately - power it on and off.

More information on RackLink™:

https://www.middleatlantic.com/racklink-technology.aspx 

More information on technical features of the devices: 

https://www.middleatlantic.com/-/media/middleatlantic/documents/techdocs/racklink%20remote%20power%20management%20system_96-01165/96_01165_rlnk.ashx 
