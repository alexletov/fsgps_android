// IPositionUpdate.aidl
package ru.alexletov.fsgps.api;

import ru.alexletov.fsgps.helpers.PositionGpsInfo;

interface IPositionUpdate {
    boolean update(in PositionGpsInfo info);
}
