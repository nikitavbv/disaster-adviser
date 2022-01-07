import {useEffect, useReducer, useRef} from 'react';
import {Button, Grid, Card, CardContent, Typography} from '@mui/material';
import moment from 'moment';
import {getGoogleToken} from '../utils';

type Disaster = {
    id: string,
    title: string,
    location: Location[],
    startDate: string | undefined,
    endDate: string | undefined,
};

type AppState = {
    disasters: Disaster[],
};

type Location = {
    latitude: number,
    longitude: number,
};

type Action = {
    type: 'ADD_DISASTER',
    disaster: Disaster,
} | { type: 'CLEAR' };

const initialState: AppState = {
    disasters: [],
};

export const HomePage = () => {
    const ws = useRef<WebSocket | null>(null);
    const [state, dispatch] = useReducer(reducer, initialState);

    useEffect(() => {
        dispatch({ type: 'CLEAR' });

        ws.current = new WebSocket('ws://localhost:8080/ws');
        ws.current.onmessage = msg => {
            const data = JSON.parse(msg.data);
            dispatch({ type: 'ADD_DISASTER', disaster: data });
        };

        const wsCurrent = ws.current;
        return () => wsCurrent.close();
    }, []);

    return (
        <Grid container spacing={2}>
            <Grid item xs={3}>
                <Typography variant='h1' sx={{fontSize: 26}} style={{padding: '8px 0'}}>Disaster Feed</Typography>
                { state.disasters.sort((a, b) => {
                    const aDate = a.startDate !== undefined ? Date.parse(a.startDate) : Date.now();
                    const bDate = b.startDate !== undefined ? Date.parse(b.startDate) : Date.now();
                    return bDate - aDate;
                }).map(disaster => <DisasterCard key={disaster.id} disaster={disaster} />) }
            </Grid>
            <Grid item xs={9}>
                <Typography variant='h1' sx={{fontSize: 26}} style={{padding: '8px 0'}}>Your events</Typography>
                <Button onClick={() => getGoogleToken().then(console.log)} variant='contained'>Connect to Google Calendar</Button>
            </Grid>
        </Grid>
    );
};

export const DisasterCard = (props: { disaster: Disaster }) => {
    return (
        <Card style={{ marginBottom: '8px' }}>
            <CardContent>
                <Typography variant='h5' component='div' sx={{ fontSize: 18 }}>
                    { props.disaster.title }
                </Typography>
                <Typography variant='h5' component='div' sx={{ fontSize: 12 }}>
                    { props.disaster.location.map(location => location.latitude + ", " + location.longitude).join("; ") }
                </Typography>
                <Typography variant='h5' component='div' sx={{ fontSize: 14 }}>
                    { props.disaster.startDate === undefined ? 'Currently active' : moment(props.disaster.startDate).fromNow() }
                </Typography>
            </CardContent>
        </Card>
    );
}

const reducer = (state: AppState, action: Action) => {
    switch (action.type) {
        case 'CLEAR':
            return initialState;
        case 'ADD_DISASTER':
            return {
                ...state,
                disasters: [
                    action.disaster,
                    ...state.disasters
                ],
            };
        default:
            return state;
    }
};