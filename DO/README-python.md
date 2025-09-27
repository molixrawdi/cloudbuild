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

Do you want me to show you how this works in practice with a full example using both radius (public) and _radius (private) so you can see the difference?