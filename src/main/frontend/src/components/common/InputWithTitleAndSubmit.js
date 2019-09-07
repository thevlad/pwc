import React from 'react';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import TextField from '@material-ui/core/TextField';
import { makeStyles } from '@material-ui/core/styles';
import './InputWithTitleAndSubmit.css';

// { title, label, disabled, setValue, submitInput, inputReference, titleRef,initialValue,handleKeyDown }

function InputWithTitleAndSubmit({ title, label, disabled, setValue, submitInput, inputReference, titleRef, initialValue, handleKeyDown, error, errorMessage }) {

    const useStyles = makeStyles(theme => ({
        container: {
            display: 'flex',
            flexWrap: 'wrap',
            width: 500,
        },
        textField: {
            marginLeft: theme.spacing(1),
            marginRight: theme.spacing(1),
            width: '100%',
        },
        dense: {
            marginTop: theme.spacing(2),
        },
        menu: {
            width: 200,
        },
    }));

    const classes = useStyles();

    return (
        <div className="search-div">
            <Typography variant="h4" color="primary">
                <span ref={titleRef}>{title}</span>
            </Typography>
            <TextField
                label={label}
                type="search"
                className={classes.textField}
                margin="normal"
                variant="outlined"
                inputRef={inputReference}
                onChange={setValue}
                onKeyDown={handleKeyDown}
                width="400px"
                disabled={disabled}
                value={initialValue}
                autoFocus={true}
                error={error}
                helperText={errorMessage}
            />
            <Button variant="contained" color="primary" onClick={submitInput} disabled={disabled}>
                Go
            </Button>
        </div>
    )
};

export default InputWithTitleAndSubmit;