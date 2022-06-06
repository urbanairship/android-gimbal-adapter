# Android Airship Gimbal Adapter ChangeLog

## Version 7.4.0 - May 31, 2022
- Updated Airship SDK to 16.5.0
- Updated Gimbal SDK to 4.7.0

## Version 7.3.0 - January 5, 2022
- Updated Airship SDK to 16.1.1
- Updated Gimbal SDK to 4.6.0

## Version 7.2.0 - August 10, 2021
- Updated Airship SDK to 14.6.0
- Updated Gimbal SDK to 4.5.2

## Version 7.1.0 - March 9, 2021
- Updated Airship SDK to 14.2.0
- Updated Gimbal SDK to 4.5.0

## Version 7.0.0 - June 12, 2020
- Removed gimbalApiKey parameters from start methods. Applications should set the API key directly
  on the Gimbal instance in Application#onCreate or use the new `enableGimbalApiKeyManagement(String)`
  method to allow the adapter to manage setting the Gimbal API key.
  
## Version 6.1.0 - May 6, 2020
- Updated Airship SDK to 13.1.0
- Added missing nullability annotations

## Version 6.0.0 - April 22, 2020
- Updated Airship SDK to 13.0.0

## Version 5.0.0 - January 9, 2020
- Updated Airship SDK to 12.1.0

## Version 4.1.1 - December 11, 2019
- Fixed null pointer exception due to updating Gimbal attributes when the adapter is stopped.

## Version 4.1.0 - November 1, 2019
- Updated Airship SDK to 11.0.5.
- Updated Gimbal SDK to 4.2.1
- Catch all exceptions thrown from the Gimbal SDK that originate from the Adapter.

## Version 4.0.0 - October 11, 2019
- Updated Airship SDK to 11.0.3
- Don't stop Gimbal if the adapter is not started.
- Handle exception thrown when Gimbal.stop() is called when Gimbal is not started.

## Version 3.0.0 - June 21, 2019
- Updated Airship SDK to 10.0.1
- Updated Gimbal SDK to 4.0.1

## Version 2.2.0 - March 14, 2019
Fixed a security issue within Android Urban Airship SDK dependency, that could allow trusted URL redirects in
certain edge cases. All applications that are using version 2.1.0 should update as soon as possible.
For more details, please email security@urbanairship.com.

- Updated Urban Airship SDK to 9.7.1
- Updated Gimbal SDK to 3.2.3

## Version 2.1.0 - April 25, 2018
- Updated Urban Airship SDK to 9.1.0.

## Version 2.0.1 March 15, 2018
- Updated Gimbal SDK to 3.1.1

## Version 2.0.0 January 9, 2017
- Updated to Urban Airship SDK 8.9.6
- Updated to Gimbal SDK 3.0

## Version 1.1.1 December 7, 2017
- Fix Gimbal initialization

## Version 1.1.0 June 6, 2017
- Updated Urban Airship SDK to 8.5.1
- Added device attribute tracking between Urban Airship and Gimbal SDKs

## Version 1.0.0 December 2, 2016
- Initial Gimbal Adapter release
