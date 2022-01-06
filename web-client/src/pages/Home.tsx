import {Button} from "@material-ui/core";
import {getGoogleToken} from "../utils";

export const HomePage = () => {
    return (
        <h1>
            Hello World
            <Button onClick={() => getGoogleToken().then(console.log)}>Connect to Google Calendar</Button>
        </h1>
    );
};