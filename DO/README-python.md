Example of real world python generators

```

def log(func):
    def wrapper(*args, **kwargs):
        print(f"Calling {func.__name__}")
        return func(*args, **kwargs)
    return wrapper

@log
def add(a, b):
    return a + b

print(add(3, 4))

```

question:

what does this mean self._radius = radius

Example Below

```
class Circle:
    def __init__(self, radius):
        self._radius = radius

    @property
    def area(self):
        return 3.1416 * self._radius ** 2

c = Circle(5)
print(c.area)  # Computed like a method, accessed like an attribute

```


Note: 
self.radius = radius: public attribute, anyone can access or modify it.

self._radius = radius: “internal” attribute, conventionally private. Often paired with @property to provide controlled access.

Showing audience how this works in practice with a full example using both radius (public) and _radius (private) so you can see the difference


public example:

```

class Circle:
    def __init__(self, radius):
        self.radius = radius  # public attribute

    def area(self):
        return 3.1416 * self.radius ** 2


c = Circle(5)
print(c.radius)   # ✅ directly accessible
c.radius = -10    # ⚠️ possible, but doesn't make sense for a circle
print(c.area())   # ❌ gives a wrong area because radius shouldn't be negative

```

Private example

```

class Circle:
    def __init__(self, radius):
        self._radius = radius  # private-by-convention

    @property
    def radius(self):
        """Getter for radius"""
        return self._radius

    @radius.setter
    def radius(self, value):
        """Setter with validation"""
        if value <= 0:
            raise ValueError("Radius must be positive!")
        self._radius = value

    def area(self):
        return 3.1416 * self._radius ** 2


c = Circle(5)
print(c.radius)   # ✅ looks like normal access (actually goes through getter)

c.radius = 10     # ✅ works, sets via setter
print(c.area())

c.radius = -3     # ❌ raises ValueError: Radius must be positive!

```


Details:

What’s happening in Example 2

The real data is stored in self._radius (internal).

We expose a radius property that:

Lets us read it via c.radius (getter).

Lets us write it via c.radius = value (setter), but only if valid.

This way, the outside world thinks it’s dealing with a simple attribute (c.radius), but we actually have control and validation behind the scenes.

Summary:

self.radius → public, no restrictions.

self._radius + @property → private (convention), with controlled access.