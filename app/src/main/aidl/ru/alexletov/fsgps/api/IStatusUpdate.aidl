// IStatusUpdate.aidl
package ru.alexletov.fsgps.api;

import ru.alexletov.fsgps.helpers.StatusInfo;

interface IStatusUpdate {
    boolean update(in StatusInfo status);
}