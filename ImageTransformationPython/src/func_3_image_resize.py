import base64
import os



def handle_request(event, context = None):
    pass


# Some testing code.
if __name__ == '__main__':
    print("\nPWD: " + os.getcwd())
    image_name = 'github-logo.png'
    with open('../sample images/' + image_name, 'rb') as f:
        event_obj = {'image_file': base64.b64encode(f.read())}
        print(handle_request(event_obj))