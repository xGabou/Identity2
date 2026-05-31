package net.Gabou.identity2.util;

public interface LimbAnimatorAccessor {
    void setPrevSpeed(float lastSpeed);

    float getPrevSpeed();

    void setPosition(float position);

    float getPosition();

    void setPositionScale(float positionScale);

    float getPositionScale();
}
