// GpsServiceApi.aidl
package ru.alexletov.fsgps;

import ru.alexletov.fsgps.helpers.PositionGpsInfo;
import ru.alexletov.fsgps.helpers.StatusInfo;
import ru.alexletov.fsgps.api.IPositionUpdate;
import ru.alexletov.fsgps.api.IStatusUpdate;

interface IGpsServiceApi {
    void startEmulate();
    void stopEmulate();
    void registerPositionCallback(in IPositionUpdate callback);
    void registerStatusCallback(in IStatusUpdate callback);
    void unregisterPositionCallback();
    void unregisterStatusCallback();
    PositionGpsInfo getPosition();
    StatusInfo getStatus();
}
